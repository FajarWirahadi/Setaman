package com.example.florist.repository;

import com.example.florist.model.Complaint;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class ComplaintRepository {

    public interface OnActionCallback {
        void onSuccess();
        void onError(String message);
    }
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void submitComplaint(String rentalId, Complaint complaint, OnActionCallback callback) {
        WriteBatch writeBatch = firestore.batch();

        DocumentReference complaintRef = firestore.collection("rentals")
                .document(rentalId)
                .collection("complaints")
                .document(complaint.getComplaintId());
        writeBatch.set(complaintRef, complaint);

        DocumentReference rentalRef = firestore.collection("rentals").document(rentalId);
        writeBatch.update(rentalRef, "status", "Komplain", "hasComplaint", true);

        writeBatch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(task.getException().getMessage());
            }
        });
    }
}
