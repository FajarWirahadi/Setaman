const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const midtransClient = require("midtrans-client");

admin.initializeApp();
const db = admin.firestore();


const snap = new midtransClient.Snap({
    isProduction: false,
    serverKey: process.env.MIDTRANS_SERVER_KEY,
    clientKey: process.env.MIDTRANS_CLIENT_KEY
});

const coreApi = new midtransClient.CoreApi({
    isProduction: false,
    serverKey: process.env.MIDTRANS_SERVER_KEY,
    clientKey: process.env.MIDTRANS_CLIENT_KEY
});


exports.generateMidtransToken = functions
    .region("asia-southeast2")
    .firestore
    .document("orders/{orderId}")
    .onCreate(async (snapshot, context) => {
        const orderData = snapshot.data();
        const orderId = context.params.orderId;

        if (orderData.paymentMethod === "COD") {
            console.log(`Order ${orderId} pakai COD. Melewati Midtrans.`);
            return null;
        }

        try {
            const parameter = {
                "transaction_details": {
                    "order_id": orderId,
                    "gross_amount": Math.round(orderData.totalAmount || 0)
                },
                "customer_details": {
                    "first_name": orderData.buyerName || "Pelanggan Setaman",
                    "email": orderData.buyerEmail || "dummy@email.com",
                    "phone": orderData.buyerPhone || "0800000000"    
                },
                "custom_expiry" : {
                    "expiry_duration" : 60,
                    "unit": "minute"
                }
            };

            const transaction = await snap.createTransaction(parameter);
            const transactionToken = transaction.token;

            await db.collection("orders").doc(orderId).update({
                snapToken: transactionToken
            });

            console.log(`Berhasil generate Snap Token untuk order: ${orderId}`);
            return null;
        } catch (error) {
            console.error("Gagal membuat Midtrans token:", error);
            
            await db.collection("orders").doc(orderId).update({
                snapToken: "ERROR_DARI_SERVER"
            });
            
            return null;
        }
    });


exports.midtransWebhook = functions
    .region("asia-southeast2")
    .https.onRequest(async (req, res) => {
        try {
            const notificationJson = req.body;
            const statusResponse = await coreApi.transaction.notification(notificationJson);

            const orderId = statusResponse.order_id;
            const transactionStatus = statusResponse.transaction_status;
            const fraudStatus = statusResponse.fraud_status;

            console.log(`Notifikasi untuk Order ID: ${orderId}, Status: ${transactionStatus}`);

            const orderRef = db.collection('orders').doc(orderId);
            const orderDoc = await orderRef.get();

            if (!orderDoc.exists) {
                console.log("Error: Order tidak ditemukan di database!");
                return res.status(404).send("Not Found");
            }

            const orderData = orderDoc.data();

            if (transactionStatus === 'capture' || transactionStatus === 'settlement') {
                if (fraudStatus === 'accept' || !fraudStatus) {
                    
                    await orderRef.update({ 
                        status: 'Menunggu Konfirmasi',
                        paymentTime: admin.firestore.FieldValue.serverTimestamp()
                    });
                    

                    await sendNotificationToSeller(orderData, orderId);
                }
            } else if (transactionStatus === 'cancel' || transactionStatus === 'deny' || transactionStatus === 'expire') {
                await orderRef.update({ status: 'Dibatalkan' });
            }

            res.status(200).send("OK");
        } catch (error) {
            console.error("Webhook Error:", error);
            res.status(500).send("Internal Server Error");
        }
    });

exports.handleOrderRejected = functions
    .region("asia-southeast2")
    .firestore
    .document("orders/{orderId}")
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const after = change.after.data();
        const orderId = context.params.orderId;

        if (before.status === "Menunggu Konfirmasi" && after.status === "Dibatalkan") {
            console.log(`Order ${orderId} ditolak penjual. Memulai proses Refund Midtrans...`);
            
            try {
                const parameter = {
                    "refund_key": "refund-" + orderId,
                    "amount": after.totalAmount,
                    "reason": after.cancellationReason || "Ditolak oleh penjual tanpa alasan."
                };
            
                const refundResponse = await coreApi.transaction.refundDirect(orderId, parameter);
                console.log(`Refund berhasil untuk Order ${orderId}:`, refundResponse);
                
                await change.after.ref.update({ 
                    refundStatus: "SUKSES",
                    refundTime: admin.firestore.FieldValue.serverTimestamp()
                });
                
            } catch (error) {
                console.error(`Refund GAGAL untuk Order ${orderId}:`, error);
                
                await change.after.ref.update({ 
                    refundStatus: "GAGAL",
                    refundError: error.message 
                });
            }
        }
        
        return null;
    });

    // ============================================================================
// FUNGSI: MENGHITUNG RATA-RATA RATING PRODUK SECARA OTOMATIS
// ============================================================================
exports.calculateProductAverageRating = functions
    .region("asia-southeast2")
    .firestore
    .document("products/{productId}/reviews/{reviewId}")
    .onWrite(async (change, context) => {
        const productId = context.params.productId;
        
        const reviewsRef = db.collection("products").doc(productId).collection("reviews");

        try {
            const snapshot = await reviewsRef.get();
            let totalRating = 0;
            let reviewCount = snapshot.size;

           
            if (reviewCount === 0) {
                await db.collection("products").doc(productId).update({
                    rating: 0,
                    reviewCount: 0
                });
                console.log(`Produk ${productId} sekarang tidak memiliki ulasan (0).`);
                return null;
            }

            snapshot.forEach(doc => {
                const data = doc.data();
            
                totalRating += (typeof data.rating === 'number' ? data.rating : 0);
            });

            const averageRating = totalRating / reviewCount;

        
            await db.collection("products").doc(productId).update({
                rating: Number(averageRating.toFixed(1)), 
                reviewCount: reviewCount
            });

            console.log(`Berhasil update rating Produk ${productId} -> Rata-rata: ${averageRating.toFixed(1)} dari ${reviewCount} ulasan.`);
            return null;

        } catch (error) {
            console.error(`Gagal menghitung rating untuk produk ${productId}:`, error);
            return null;
        }
    });

async function sendNotificationToSeller(orderData, orderId) {
    const sellerId = orderData.sellerId;
    
    if (!sellerId) return console.log("Gagal Notif: ID Penjual tidak ditemukan di data order.");

    const userDoc = await db.collection("users").doc(sellerId).get();
    if (!userDoc.exists) return console.log("Gagal Notif: Data Penjual tidak ditemukan.");
    
    const fcmToken = userDoc.data().fcmToken;
    if (!fcmToken) return console.log("Gagal Notif: Penjual belum mendaftarkan Token FCM.");

    const payload = {
        notification: {
            title: "Pesanan Baru Lunas! 💸🌸",
            body: `Cek toko sekarang! Ada pesanan lunas dari ${orderData.buyerName || 'pelanggan'}. Segera proses dan siapkan tanamannya!`
        },

        data: {
            type: "new_order",
            orderId: String(orderId) 
        },
   
        token: fcmToken
    };

    try {
        const response = await admin.messaging().send(payload);
        console.log("FCM Berhasil: Notifikasi pesanan lunas terkirim ke Penjual.", response);
    } catch (error) {
        console.error("FCM Gagal: Error saat mengirim notifikasi:", error);
    }
}
exports.notifyBuyerOnDeliveryUpdate = functions
    .region("asia-southeast2")
    .firestore
    .document("orders/{orderId}/delivery_logs/{logId}")
    .onCreate(async (snapshot, context) => {
        const logData = snapshot.data();
        const orderId = context.params.orderId;

        console.log(`Mendeteksi update pengantaran baru untuk Order: ${orderId}`);

        try {
            const orderDoc = await db.collection("orders").doc(orderId).get();
            if (!orderDoc.exists) {
                console.log("Gagal Notif: Dokumen order tidak ditemukan.");
                return null;
            }
            
            const buyerId = orderDoc.data().buyerId;
            if (!buyerId) {
                console.log("Gagal Notif: Order tidak memiliki buyerId.");
                return null;
            }

            // 2. Ambil data User (Pembeli) untuk mendapatkan FCM Token
            const userDoc = await db.collection("users").doc(buyerId).get();
            if (!userDoc.exists) {
                console.log("Gagal Notif: Data pembeli tidak ditemukan di database.");
                return null;
            }

            const fcmToken = userDoc.data().fcmToken;
            if (!fcmToken) {
                console.log(`Gagal Notif: Pembeli (${buyerId}) belum mendaftarkan Token FCM.`);
                return null;
            }

            const bodyText = (logData.description && logData.description.trim() !== "") 
                             ? logData.description 
                             : `Pesananmu memasuki tahap: ${logData.statusTitle}. Buka aplikasi untuk melacak!`;

            const payload = {
                notification: {
                    title: `Update Pengantaran: ${logData.statusTitle} 🚚`,
                    body: bodyText
                },
                data: {
                    type: "delivery_update",
                    orderId: String(orderId)
                },
                token: fcmToken
            };

            const response = await admin.messaging().send(payload);
            console.log("Notifikasi Berhasil: Notifikasi update pengantaran terkirim ke Pembeli.", response);
            return null;

        } catch (error) {
            console.error("Notifikasi Error: Terjadi kesalahan saat memproses notifikasi pembeli:", error);
            return null;
        }
    });

    exports.sendChatNotification = functions.region('asia-southeast2').firestore
    .document('chat_rooms/{roomId}/messages/{messageId}')
    .onCreate(async (snap, context) => {
        const messageData = snap.data();
        const roomId = context.params.roomId;
        const senderId = messageData.senderId;
        const text = messageData.text;

        try {
            let senderName = "Pengguna Setaman";
            
            const shopDoc = await admin.firestore().collection('shops').doc(senderId).get();
            if (shopDoc.exists) {
                senderName = shopDoc.data().shopName || "Penjual";
            } else {
                const userDoc = await admin.firestore().collection('users').doc(senderId).get();
                if (userDoc.exists) {
                    senderName = userDoc.data().username || userDoc.data().name || "Pembeli";
                }
            }

            const roomDoc = await admin.firestore().collection('chat_rooms').doc(roomId).get();
            if (!roomDoc.exists) return null;
            const roomData = roomDoc.data();

            const receiverId = (senderId === roomData.buyerId) ? roomData.sellerId : roomData.buyerId;
        
            const userDocReceiver = await admin.firestore().collection('users').doc(receiverId).get();
            if (!userDocReceiver.exists) return null;
            
            const fcmToken = userDocReceiver.data().fcmToken;
            if (!fcmToken) return null;

        
            const message = {
                notification: {
                    title: senderName,
                    body: text
                },
                data: {
                    type: "chat",
                    roomId: roomId,
                    targetId: senderId,      
                    targetName: senderName   
                },
                token: fcmToken
            };

            const response = await admin.messaging().send(message);
            console.log('Notifikasi chat berhasil dikirim:', response);
            return null;

        } catch (error) {
            console.error('Gagal memproses notifikasi chat:', error);
            return null;
        }
    });