package com.example.florist.repository;

import com.example.florist.model.Complaint;
import com.example.florist.model.ComplaintMessage;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ComplaintRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // --- INTERFACES UNTUK KOMUNIKASI DENGAN VIEWMODEL ---
    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ComplaintListCallback {
        void onDataFetched(List<Complaint> complaints);
        void onError(String message);
    }

    public interface ChatListCallback {
        void onDataFetched(List<ComplaintMessage> messages);
        void onError(String message);
    }

    // --- 1. SUBMIT COMPLAINT (ATOMIC BATCH) ---
    public void submitComplaintWithRentalUpdate(Complaint complaint, ActionCallback callback) {
        WriteBatch batch = db.batch();

        // Referensi Dokumen
        DocumentReference compRef = db.collection("complaints").document(complaint.getComplaintId());
        DocumentReference rentalRef = db.collection("rentals").document(complaint.getRentalId());

        // Eksekusi Batch
        batch.set(compRef, complaint);
        batch.update(rentalRef,
                "hasComplaint", true,
                "activeComplaintId", complaint.getComplaintId(),
                "status", "KOMPLAIN"
        );

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError("Gagal mengirim komplain: " + e.getMessage()));
    }

    // --- 2. FETCH COMPLAINTS (REALTIME) ---
    public ListenerRegistration listenToComplaints(String rentalId, ComplaintListCallback callback) {
        return db.collection("complaints")
                .whereEqualTo("rentalId", rentalId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        List<Complaint> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            Complaint c = doc.toObject(Complaint.class);
                            if (c != null) list.add(c);
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            list.sort(Comparator.comparing(Complaint::getCreatedAt));
                        }
                        callback.onDataFetched(list);
                    }
                });
    }

    // --- 3. ACCEPT RESOLUTION (ATOMIC BATCH) ---
    public void acceptResolution(String complaintId, ActionCallback callback) {
        DocumentReference compRef = db.collection("complaints").document(complaintId);

        compRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Complaint currentComplaint = doc.toObject(Complaint.class);
                if (currentComplaint == null) {
                    callback.onError("Data komplain korup.");
                    return;
                }

                WriteBatch batch = db.batch();
                batch.update(compRef, "status", "SELESAI", "resolvedAt", Timestamp.now());

                DocumentReference rentalRef = db.collection("rentals").document(currentComplaint.getRentalId());
                batch.update(rentalRef,
                        "hasComplaint", false,
                        "activeComplaintId", null,
                        "status", "SEWA AKTIF"
                );

                batch.commit()
                        .addOnSuccessListener(task -> callback.onSuccess())
                        .addOnFailureListener(e -> callback.onError("Gagal menyelesaikan: " + e.getMessage()));
            } else {
                callback.onError("Komplain tidak ditemukan.");
            }
        }).addOnFailureListener(e -> callback.onError("Gagal membaca data: " + e.getMessage()));
    }

    // --- 4. REJECT RESOLUTION (ATOMIC BATCH) ---
    public void rejectResolution(String complaintId, String rejectionReason, ActionCallback callback) {
        DocumentReference complaintRef = db.collection("complaints").document(complaintId);

        complaintRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Complaint currentComplaint = doc.toObject(Complaint.class);
                if (currentComplaint == null) return;

                int newRejectionCount = currentComplaint.getRejectionCount() + 1;
                String nextStatus = (newRejectionCount == 1) ? "KUNJUNGAN WAJIB" : "DISPUTE ADMIN";

                WriteBatch batch = db.batch();
                batch.update(complaintRef,
                        "status", nextStatus,
                        "rejectionCount", newRejectionCount,
                        "rejectionReason", rejectionReason,
                        "resolutionType", null
                );

                DocumentReference rentalRef = db.collection("rentals").document(currentComplaint.getRentalId());
                batch.update(rentalRef, "status", nextStatus);

                batch.commit()
                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                        .addOnFailureListener(e -> callback.onError("Gagal menolak resolusi: " + e.getMessage()));
            } else {
                callback.onError("Data komplain tidak ditemukan.");
            }
        }).addOnFailureListener(e -> callback.onError("Gagal membaca server: " + e.getMessage()));
    }

    // --- 5. LISTEN CHAT DISCUSSION (REALTIME) ---
    public ListenerRegistration listenToDiscussion(String rentalId, ChatListCallback callback) {
        return db.collection("rentals").document(rentalId)
                .collection("discussion_messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        List<ComplaintMessage> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            ComplaintMessage msg = doc.toObject(ComplaintMessage.class);
                            if (msg != null) list.add(msg);
                        }
                        callback.onDataFetched(list);
                    }
                });
    }

    // --- 6. SEND CHAT MESSAGE ---
    public void sendChatMessage(String rentalId, ComplaintMessage message, ActionCallback callback) {
        CollectionReference chatRef = db.collection("rentals").document(rentalId).collection("discussion_messages");
        message.setMessageId(chatRef.document().getId());

        chatRef.document(message.getMessageId()).set(message)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError("Gagal mengirim pesan: " + e.getMessage()));
    }
}