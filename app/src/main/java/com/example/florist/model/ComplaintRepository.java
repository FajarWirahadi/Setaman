package com.example.florist.model;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class ComplaintRepository {

    public interface OnActionCallback {
        void onSuccess();
        void onError(String message);
    }
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void submitComplaint(String orderId, Complaint complaint, OnActionCallback callback) {
        WriteBatch writeBatch = firestore.batch();

        DocumentReference complaintRef = firestore.collection("orders")
                .document(orderId)
                .collection("complaints")
                .document(complaint.getComplaintId());
        writeBatch.set(complaintRef, complaint);

        DocumentReference orderRef = firestore.collection("orders").document(orderId);
        writeBatch.update(orderRef,"status", "Komplain");

        writeBatch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(task.getException().getMessage());
            }
        });
    }
}
