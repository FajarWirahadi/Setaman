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

// ============================================================================
// 1. GENERATOR TOKEN MIDTRANS
// ============================================================================
exports.generateMidtransToken = functions.region("asia-southeast2").firestore
    .document("orders/{orderId}").onCreate(async (snapshot, context) => {
        const orderData = snapshot.data();
        const orderId = context.params.orderId;
        if (orderData.paymentMethod === "COD") return null;

        try {
            const parameter = {
                "transaction_details": { "order_id": orderId, "gross_amount": Math.round(orderData.totalAmount || 0) },
                "customer_details": { "first_name": orderData.buyerName || "Pelanggan", "email": orderData.buyerEmail || "dummy@email.com", "phone": orderData.buyerPhone || "0800000000" },
                "custom_expiry" : { "expiry_duration" : 60, "unit": "minute" }
            };
            const transaction = await snap.createTransaction(parameter);
            await db.collection("orders").doc(orderId).update({ snapToken: transaction.token });
            return null;
        } catch (error) {
            console.error("Gagal membuat Midtrans token:", error);
            await db.collection("orders").doc(orderId).update({ snapToken: "ERROR_DARI_SERVER" });
            return null;
        }
    });

exports.generateExtensionToken = functions.region("asia-southeast2").firestore
    .document("extensions/{extId}").onCreate(async (snapshot, context) => {
        const data = snapshot.data();
        const extId = context.params.extId;
        try {
            const parameter = {
                "transaction_details": { "order_id": extId, "gross_amount": Math.round(data.amount || 0) },
                "customer_details": { "first_name": data.buyerName || "Pelanggan", "email": "extend@setaman.com", "phone": "080000" },
                "item_details": [{ "id": data.rentalId, "price": Math.round(data.amount || 0), "quantity": 1, "name": `Perpanjang (${data.extensionDays} Hari)` }],
                "custom_expiry" : { "expiry_duration" : 60, "unit": "minute" }
            };
            const transaction = await snap.createTransaction(parameter);
            await db.collection("extensions").doc(extId).update({ snapToken: transaction.token, redirectUrl: transaction.redirect_url });
            return null;
        } catch (error) {
            console.error("Gagal membuat token perpanjangan:", error);
            await db.collection("extensions").doc(extId).update({ snapToken: "ERROR" });
            return null;
        }
    });

// ============================================================================
// 2. WEBHOOK PEMBAYARAN MIDTRANS
// ============================================================================
exports.midtransWebhook = functions.region("asia-southeast2").https.onRequest(async (req, res) => {
    try {
        const statusResponse = await coreApi.transaction.notification(req.body);
        const orderId = statusResponse.order_id;
        const transactionStatus = statusResponse.transaction_status;
        const fraudStatus = statusResponse.fraud_status;

        // TAGIHAN PERPANJANGAN
        if (orderId.startsWith("EXT-")) {
            const extRef = db.collection('extensions').doc(orderId);
            const extDoc = await extRef.get();
            if (!extDoc.exists) return res.status(404).send("Not Found");
            
            if (transactionStatus === 'capture' || transactionStatus === 'settlement') {
                if (fraudStatus === 'accept' || !fraudStatus) {
                    await extRef.update({ status: 'LUNAS', paymentTime: admin.firestore.FieldValue.serverTimestamp() });
                    const extensionDays = extDoc.data().extensionDays || 30;
                    const rentalRef = db.collection('rentals').doc(extDoc.data().rentalId);
                    const rentalDoc = await rentalRef.get();
                    if (rentalDoc.exists) {
                        const newEndDate = new Date(rentalDoc.data().endDate.toDate().getTime() + (extensionDays * 24 * 60 * 60 * 1000));
                        await rentalRef.update({ endDate: admin.firestore.Timestamp.fromDate(newEndDate), status: "SEWA AKTIF", systemNote: `Perpanjangan sewa lunas. Ditambah ${extensionDays} hari.` }); // UPPERCASE
                    }
                }
            } else if (transactionStatus === 'cancel' || transactionStatus === 'deny' || transactionStatus === 'expire') {
                await extRef.update({ status: 'DIBATALKAN/EXPIRED' }); 
            }
            return res.status(200).send("OK");
        }

        // PESANAN BARU
        const orderRef = db.collection('orders').doc(orderId);
        const orderDoc = await orderRef.get();
        if (!orderDoc.exists) return res.status(404).send("Not Found");

        if (transactionStatus === 'capture' || transactionStatus === 'settlement') {
            if (fraudStatus === 'accept' || !fraudStatus) {
                await orderRef.update({ status: 'MENUNGGU KONFIRMASI', paymentTime: admin.firestore.FieldValue.serverTimestamp() });
                
                const rentalsSnapshot = await db.collection('rentals').where('orderId', '==', orderId).get();
                if (!rentalsSnapshot.empty) {
                    const batch = db.batch();
                    rentalsSnapshot.forEach(doc => {
                        batch.update(doc.ref, { status: 'MENUNGGU KONFIRMASI' });
                    });
                    await batch.commit();
                }

                await sendNotificationToSeller(orderDoc.data(), orderId);
            }
        } else if (transactionStatus === 'cancel' || transactionStatus === 'deny' || transactionStatus === 'expire') {
            await orderRef.update({ status: 'DIBATALKAN' });
        
            const rentalsSnapshot = await db.collection('rentals').where('orderId', '==', orderId).get();
            if (!rentalsSnapshot.empty) {
                const batch = db.batch();
                rentalsSnapshot.forEach(doc => {
                    batch.update(doc.ref, { status: 'DIBATALKAN' });
                });
                await batch.commit();
            }
        }
        res.status(200).send("OK");
    } catch (error) {
        res.status(500).send("Error");
    }
});

// ============================================================================
// 3. FITUR LOGISTIK & TRANSAKSI
// ============================================================================
exports.handleOrderRejected = functions.region("asia-southeast2").firestore.document("orders/{orderId}").onUpdate(async (change, context) => {
    const before = change.before.data(), after = change.after.data(), orderId = context.params.orderId;
    if (before.status === "MENUNGGU KONFIRMASI" && after.status === "DIBATALKAN") { // UPPERCASE
        try {
            const parameter = { "refund_key": "refund-" + orderId, "amount": after.totalAmount, "reason": after.cancellationReason || "Ditolak penjual." };
            await coreApi.transaction.refundDirect(orderId, parameter);
            await change.after.ref.update({ refundStatus: "SUKSES", refundTime: admin.firestore.FieldValue.serverTimestamp() });
        } catch (error) {
            await change.after.ref.update({ refundStatus: "GAGAL", refundError: error.message });
        }
    }
    return null;
});

exports.calculateProductAverageRating = functions.region("asia-southeast2").firestore.document("products/{productId}/reviews/{reviewId}").onWrite(async (change, context) => {
    const productId = context.params.productId;
    const snapshot = await db.collection("products").doc(productId).collection("reviews").get();
    if (snapshot.empty) {
        await db.collection("products").doc(productId).update({ rating: 0, reviewCount: 0 });
        return null;
    }
    let totalRating = 0;
    snapshot.forEach(doc => totalRating += (typeof doc.data().rating === 'number' ? doc.data().rating : 0));
    const averageRating = totalRating / snapshot.size;
    await db.collection("products").doc(productId).update({ rating: Number(averageRating.toFixed(1)), reviewCount: snapshot.size });
    return null;
});

exports.notifyBuyerOnDeliveryUpdate = functions.region("asia-southeast2").firestore.document("orders/{orderId}/delivery_logs/{logId}").onCreate(async (snapshot, context) => {
    const logData = snapshot.data();
    const orderDoc = await db.collection("orders").doc(context.params.orderId).get();
    if (!orderDoc.exists) return null;
    const userDoc = await db.collection("users").doc(orderDoc.data().buyerId).get();
    if (userDoc.exists && userDoc.data().fcmToken) {
        await admin.messaging().send({
            notification: { title: `Update Pengantaran: ${logData.statusTitle} 🚚`, body: logData.description || `Pesananmu memasuki tahap: ${logData.statusTitle}.` },
            data: { type: "delivery_update", orderId: String(context.params.orderId) },
            token: userDoc.data().fcmToken
        });
    }
    return null;
});

exports.sendChatNotification = functions.region('asia-southeast2').firestore.document('chat_rooms/{roomId}/messages/{messageId}').onCreate(async (snap, context) => {
    const msg = snap.data();
    const roomDoc = await db.collection('chat_rooms').doc(context.params.roomId).get();
    if (!roomDoc.exists) return null;
    
    const roomData = roomDoc.data();
    const receiverId = (msg.senderId === roomData.buyerId) ? roomData.sellerId : roomData.buyerId;
    const receiverDoc = await db.collection('users').doc(receiverId).get();
    
    if (receiverDoc.exists && receiverDoc.data().fcmToken) {
        const senderName = (msg.senderId === roomData.buyerId) ? roomData.buyerName : roomData.sellerName;
        
        await admin.messaging().send({
            data: { 
                title: String(senderName || "Pesan Baru"), 
                body: String(msg.text || "📷 Mengirim gambar"),
                type: "chat", 
                roomId: String(context.params.roomId), 
                targetId: String(msg.senderId), 
                targetName: String(senderName) 
            },
            token: receiverDoc.data().fcmToken
        });
    }
    return null;
});
exports.notifyMaintenanceUpdate = functions.region("asia-southeast2").firestore.document("rentals/{rentalId}/maintenance_logs/{logId}").onCreate(async (snapshot, context) => {
    const logData = snapshot.data();
    const rentalDoc = await db.collection("rentals").doc(context.params.rentalId).get();
    if (!rentalDoc.exists) return null;
    const userDoc = await db.collection("users").doc(rentalDoc.data().buyerId).get();
    if (userDoc.exists && userDoc.data().fcmToken) {
        const isComplaint = logData.description && logData.description.includes("RESOLUSI KOMPLAIN");
        await admin.messaging().send({
            notification: { title: isComplaint ? "Perbaikan Selesai! 🛠️" : "Laporan Perawatan Baru 🌿", body: "Cek laporan dari florist." },
            data: { type: "rental_update", rentalId: String(context.params.rentalId) },
            token: userDoc.data().fcmToken
        });
    }
    return null;
});

exports.notifyComplaintTriggers = functions.region("asia-southeast2").firestore.document("complaints/{complaintId}").onWrite(async (change, context) => {
    if (!change.after.exists) return null;
    const after = change.after.data(), before = change.before.exists ? change.before.data() : null;
    const status = after.status ? after.status.toUpperCase() : "", oldStatus = before && before.status ? before.status.toUpperCase() : "";
    if (before && status === oldStatus) return null;

    let targetId = null, title = "", body = "";
    if (!before) { targetId = after.sellerId; title = "⚠️ Komplain Baru Masuk!"; body = "Pelanggan mengajukan komplain."; } 
    else if (status === "KUNJUNGAN WAJIB") { targetId = after.sellerId; title = "🚨 Resolusi Ditolak!"; body = "Pelanggan menolak panduan. Lakukan kunjungan fisik."; } 
    else if (status === "MENUNGGU KONFIRMASI") { targetId = after.buyerId; title = "✅ Update Komplain"; body = "Florist telah memberi solusi."; }

    if (targetId) {
        const userDoc = await db.collection("users").doc(targetId).get();
        if (userDoc.exists && userDoc.data().fcmToken) {
            await admin.messaging().send({
                notification: { title, body }, data: { type: "rental_update", rentalId: String(after.rentalId) }, token: userDoc.data().fcmToken
            });
        }
    }
    return null;
});

async function sendNotificationToSeller(orderData, orderId) {
    const userDoc = await db.collection("users").doc(orderData.sellerId).get();
    if (userDoc.exists && userDoc.data().fcmToken) {
        await admin.messaging().send({
            notification: { title: "Pesanan Baru Lunas! 💸🌸", body: `Pesanan lunas dari ${orderData.buyerName || 'pelanggan'}.` },
            data: { type: "new_order", orderId: String(orderId) }, token: userDoc.data().fcmToken
        });
    }
}

// ============================================================================
// 4. CRON JOBS DENGAN STANDAR UPPERCASE
// ============================================================================
exports.autoCompleteRentals = functions.region("asia-southeast2").pubsub.schedule("0 1 * * *").timeZone("Asia/Jakarta").onRun(async (context) => {
    const today = new Date(); today.setHours(0, 0, 0, 0); 
    const snapshot = await db.collection("rentals").where("status", "==", "SEWA AKTIF").get();
    if (snapshot.empty) return null;
    const batch = db.batch();
    snapshot.forEach(doc => {
        if (doc.data().endDate) {
            const endDate = doc.data().endDate.toDate(); endDate.setHours(0, 0, 0, 0);
            if (endDate < today) batch.update(doc.ref, { status: "MENUNGGU PENARIKAN", systemNote: "Masa sewa habis. Tunggu penarikan." });
        }
    });
    await batch.commit(); return null;
});

exports.notifyExpiringRentals = functions.region("asia-southeast2").pubsub.schedule("0 8 * * *").timeZone("Asia/Jakarta").onRun(async (context) => {
    const today = new Date(); today.setHours(0, 0, 0, 0);
    const snapshot = await db.collection("rentals").where("status", "==", "SEWA AKTIF").get(); 
    if (snapshot.empty) return null;
    
    snapshot.forEach(async (doc) => {
        const data = doc.data();
        if (!data.endDate) return;
        const endDate = data.endDate.toDate(); endDate.setHours(0, 0, 0, 0);
        
        const diffDays = Math.round((endDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
        if (diffDays === 3) {
            const bDoc = await db.collection("users").doc(data.buyerId).get();
            const sDoc = await db.collection("users").doc(data.sellerId).get();
            if (bDoc.exists && bDoc.data().fcmToken) await admin.messaging().send({ notification: { title: "⏳ Sewa Berakhir 3 Hari Lagi!", body: "Perpanjang sekarang!" }, data: { type: "rental_update", rentalId: String(doc.id) }, token: bDoc.data().fcmToken });
            if (sDoc.exists && sDoc.data().fcmToken) await admin.messaging().send({ notification: { title: "Peluang Perpanjangan Sewa! 💸", body: "Tawarkan perpanjangan via chat!" }, data: { type: "rental_update", rentalId: String(doc.id) }, token: sDoc.data().fcmToken });
        }
    });
    return null;
});

exports.autoResolveComplaints = functions.region("asia-southeast2").pubsub.schedule("0 0 * * *").timeZone("Asia/Jakarta").onRun(async (context) => {
    const threeDaysAgo = new Date(Date.now() - (3 * 24 * 60 * 60 * 1000));
    const snapshot = await db.collection("complaints").where("status", "==", "MENUNGGU KONFIRMASI").get(); 
    if (snapshot.empty) return null;
    
    const batch = db.batch();
    snapshot.forEach(doc => {
        const data = doc.data();
        const targetTime = data.respondedAt ? data.respondedAt.toDate() : data.createdAt.toDate();
        if (targetTime < threeDaysAgo) {
            batch.update(db.collection("complaints").doc(doc.id), { status: "SELESAI", resolvedAt: admin.firestore.FieldValue.serverTimestamp() }); 
            if (data.rentalId) batch.update(db.collection("rentals").doc(data.rentalId), { status: "SEWA AKTIF", hasComplaint: false }); 
        }
    });
    await batch.commit(); return null;
});

exports.autoCancelUnresponsiveOrders = functions.region("asia-southeast2").pubsub.schedule("0 * * * *").timeZone("Asia/Jakarta").onRun(async (context) => {
    // Cari pesanan yang usianya sudah lebih dari 24 jam
    const twentyFourHoursAgo = new Date(Date.now() - (24 * 60 * 60 * 1000));
    
    const snapshot = await db.collection("orders")
        .where("status", "==", "MENUNGGU KONFIRMASI")
        .where("createdAt", "<", admin.firestore.Timestamp.fromDate(twentyFourHoursAgo))
        .get();

    if (snapshot.empty) return null;

    const batch = db.batch();
    const notifications = [];

    snapshot.forEach(doc => {
        const orderData = doc.data();
        const orderRef = db.collection("orders").doc(doc.id);
        
        // 1. Ubah status pesanan menjadi Dibatalkan
        batch.update(orderRef, { 
            status: "DIBATALKAN", 
            cancellationReason: "Dibatalkan Otomatis: Florist tidak merespons dalam 24 jam." 
        });

        // 2. Siapkan Notifikasi untuk Pembeli (Uang akan direfund via handleOrderRejected)
        notifications.push(sendNotification(
            orderData.buyerId, 
            "Pesanan Dibatalkan ❌", 
            "Florist tidak merespons pesananmu dalam 24 jam. Saldo akan dikembalikan.",
            doc.id
        ));

        // 3. Siapkan Notifikasi Penalti untuk Florist
        notifications.push(sendNotification(
            orderData.sellerId, 
            "Pesanan Batal Otomatis 🚨", 
            `Pesanan ${doc.id} hangus karena melewati batas waktu respon!`,
            doc.id
        ));
    });

    await batch.commit();
    await Promise.all(notifications);
    return null;
});

// Helper Function untuk Push Notification
async function sendNotification(userId, title, body, orderId) {
    const userDoc = await db.collection("users").doc(userId).get();
    if (userDoc.exists && userDoc.data().fcmToken) {
        return admin.messaging().send({
            notification: { title: title, body: body },
            data: { type: "new_order", orderId: String(orderId) },
            token: userDoc.data().fcmToken
        }).catch(err => console.error(err));
    }
}