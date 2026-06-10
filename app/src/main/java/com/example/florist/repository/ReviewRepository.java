package com.example.florist.repository;

import android.net.Uri;

import com.example.florist.model.Review;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class ReviewRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface ReviewCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onError(String message);
    }

    // FUNGSI 1: Upload ke Cloudinary
    public void uploadReviewImage(Uri imageUri, UploadCallback callback) {
        com.cloudinary.android.MediaManager.get().upload(imageUri).callback(new com.cloudinary.android.callback.UploadCallback() {
            @Override
            public void onStart(String requestId) {}
            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override
            public void onSuccess(String requestId, java.util.Map resultData) {
                callback.onSuccess((String) resultData.get("secure_url"));
            }
            @Override
            public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                callback.onError(error.getDescription());
            }
            @Override
            public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {}
        }).dispatch();
    }

    public void saveReviewData(Review review, ReviewCallback callback) {
        WriteBatch batch = db.batch();

        DocumentReference reviewRef = db.collection("products")
                .document(review.getProductId())
                .collection("reviews")
                .document();
        review.setReviewId(reviewRef.getId());

        batch.set(reviewRef, review);

        DocumentReference orderRef = db.collection("orders").document(review.getOrderId());
        batch.update(orderRef, "isReviewed", true);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}