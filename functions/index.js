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
                    

                    await sendNotificationToSeller(orderData);
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

async function sendNotificationToSeller(orderData) {
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
        token: fcmToken
    };

    try {
        const response = await admin.messaging().send(payload);
        console.log("FCM Berhasil: Notifikasi pesanan lunas terkirim ke Penjual.", response);
    } catch (error) {
        console.error("FCM Gagal: Error saat mengirim notifikasi:", error);
    }
}