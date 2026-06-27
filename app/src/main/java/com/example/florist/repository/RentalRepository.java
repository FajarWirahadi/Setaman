package com.example.florist.repository;

import com.example.florist.model.Complaint;
import com.example.florist.model.ComplaintMessage;
import com.example.florist.model.MaintenanceLog;
import com.example.florist.model.Order;
import com.example.florist.model.Rental;
import com.example.florist.model.TimelineEvent;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RentalRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public interface ExtensionCallback {
        void onRedirectUrlReceived(String url);
        void onError(String message);
    }

    public interface RentalListCallback {
        void onSuccess(List<Rental> rentals);
        void onError(String message);
    }

    // 1. RESOLVE COMPLAINT (BATCH)
    public void resolveComplaint(String rentalId, String complaintId, String responseText, ActionCallback callback) {
        WriteBatch batch = db.batch();
        DocumentReference complaintRef = db.collection("complaints").document(complaintId);
        batch.update(complaintRef, "sellerResponseText", responseText, "status", "MENUNGGU KONFIRMASI", "respondedAt", Timestamp.now());

        DocumentReference rentalRef = db.collection("rentals").document(rentalId);
        batch.update(rentalRef, "status", "MENUNGGU KONFIRMASI");

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 2. FETCH ORDER
    public void fetchOrderDetail(String orderId, DataCallback<Order> callback) {
        db.collection("orders").document(orderId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) callback.onSuccess(doc.toObject(Order.class));
            else callback.onError("Order tidak ditemukan");
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 3. FETCH RENTAL
    public void fetchRental(String rentalId, DataCallback<Rental> callback) {
        db.collection("rentals").document(rentalId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Rental rental = doc.toObject(Rental.class);
                if (rental != null) rental.setRentalId(doc.getId());
                callback.onSuccess(rental);
            } else {
                callback.onError("Rental tidak ditemukan");
            }
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 4. LISTEN MAINTENANCE LOGS
    public ListenerRegistration listenToMaintenanceLogs(String rentalId, DataCallback<List<MaintenanceLog>> callback) {
        return db.collection("rentals").document(rentalId).collection("maintenance_logs")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) { callback.onError(error.getMessage()); return; }
                    if (value != null) {
                        List<MaintenanceLog> logs = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            MaintenanceLog log = doc.toObject(MaintenanceLog.class);
                            if (log != null) logs.add(log);
                        }
                        callback.onSuccess(logs);
                    }
                });
    }

    // 5. LISTEN COMPLAINTS
    public ListenerRegistration listenToComplaintDetail(String rentalId, DataCallback<List<Complaint>> callback) {
        return db.collection("complaints").whereEqualTo("rentalId", rentalId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) { callback.onError(error.getMessage()); return; }
                    if (value != null) {
                        List<Complaint> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            Complaint c = doc.toObject(Complaint.class);
                            if (c != null) list.add(c);
                        }
                        callback.onSuccess(list);
                    }
                });
    }

    // 6. UPDATE COMPLAINT STATUS (BATCH)
    public void updateComplaintStatus(String rentalId, String complaintId, String newStatus, String resolutionType, ActionCallback callback) {
        WriteBatch batch = db.batch();
        DocumentReference complaintRef = db.collection("complaints").document(complaintId);
        DocumentReference rentalRef = db.collection("rentals").document(rentalId);

        if ("CHAT_EDUCATION".equals(resolutionType)) {
            batch.update(complaintRef, "status", newStatus, "resolutionType", resolutionType, "respondedAt", Timestamp.now());
        } else if ("PHYSICAL_VISIT".equals(resolutionType)) {
            batch.update(complaintRef, "status", newStatus, "resolutionType", resolutionType, "visitScheduledAt", Timestamp.now(), "respondedAt", Timestamp.now());
        } else {
            batch.update(complaintRef, "status", newStatus);
        }
        batch.update(rentalRef, "status", newStatus);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 7. LISTEN CHAT
    public ListenerRegistration listenToDiscussion(String rentalId, DataCallback<List<ComplaintMessage>> callback) {
        return db.collection("rentals").document(rentalId).collection("discussion_messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) { callback.onError(error.getMessage()); return; }
                    if (value != null) {
                        List<ComplaintMessage> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            ComplaintMessage msg = doc.toObject(ComplaintMessage.class);
                            if (msg != null) list.add(msg);
                        }
                        callback.onSuccess(list);
                    }
                });
    }

    // 8. SEND CHAT
    public void sendChatMessage(String rentalId, ComplaintMessage message) {
        CollectionReference chatRef = db.collection("rentals").document(rentalId).collection("discussion_messages");
        message.setMessageId(chatRef.document().getId());
        chatRef.document(message.getMessageId()).set(message);
    }

    // 9. FETCH UNIFIED TIMELINE
    public void fetchUnifiedTimeline(String rentalId, DataCallback<List<TimelineEvent>> callback) {
        List<TimelineEvent> allEvents = new ArrayList<>();

        db.collection("rentals").document(rentalId).collection("maintenance_logs").get()
                .addOnSuccessListener(maintDocs -> {
                    for (DocumentSnapshot doc : maintDocs) {
                        Timestamp time = doc.getTimestamp("createdAt");
                        String desc = doc.getString("description");
                        String img = doc.getString("imageUrl");
                        int type = (desc != null && desc.contains("RESOLUSI KOMPLAIN")) ? TimelineEvent.TYPE_RESOLUTION : TimelineEvent.TYPE_ROUTINE;
                        String title = (type == TimelineEvent.TYPE_RESOLUTION) ? "Perbaikan Komplain" : "Perawatan Rutin";
                        allEvents.add(new TimelineEvent(type, doc.getId(), time, title, desc, img));
                    }

                    db.collection("complaints").whereEqualTo("rentalId", rentalId).get()
                            .addOnSuccessListener(compDocs -> {
                                for (DocumentSnapshot doc : compDocs) {
                                    Timestamp time = doc.getTimestamp("createdAt");
                                    String reason = doc.getString("reason");
                                    allEvents.add(new TimelineEvent(TimelineEvent.TYPE_COMPLAINT, doc.getId(), time, "Komplain: " + reason, doc.getString("description"), doc.getString("evidenceImageUrl")));
                                }
                                callback.onSuccess(allEvents);
                            }).addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 10. REQUEST EXTENSION
    public ListenerRegistration requestExtensionPayment(String extId, Map<String, Object> extData, ExtensionCallback callback) {
        db.collection("extensions").document(extId).set(extData)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));

        return db.collection("extensions").document(extId)
                .addSnapshotListener((value, error) -> {
                    if (value != null && value.exists()) {
                        String redirectUrl = value.getString("redirectUrl");
                        if (redirectUrl != null && !redirectUrl.isEmpty()) {
                            callback.onRedirectUrlReceived(redirectUrl);
                        }
                        if ("ERROR".equals(value.getString("snapToken"))) {
                            callback.onError("Gagal membuat tagihan Midtrans. Coba lagi.");
                        }
                    }
                });
    }
    public ListenerRegistration listenToBuyerRentals(RentalListCallback callback) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError("Harap login terlebih dahulu.");
            return null;
        }

        String buyerId = user.getUid();

        return db.collection("rentals")
                .whereEqualTo("buyerId", buyerId)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError("Gagal memuat data sewa: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<Rental> rentalList = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            Rental rental = doc.toObject(Rental.class);
                            if (rental != null) {
                                if (rental.getRentalId() == null) rental.setRentalId(doc.getId());
                                rentalList.add(rental);
                            }
                        }
                        callback.onSuccess(rentalList);
                    }
                });
    }

}