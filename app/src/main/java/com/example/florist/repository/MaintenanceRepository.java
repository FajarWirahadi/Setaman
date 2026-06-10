package com.example.florist.repository;

import android.net.Uri;

import com.example.florist.model.Rental;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaintenanceRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface RentalListCallback {
        void onSuccess(List<Rental> rentals);
        void onError(String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public void getSellerMaintenanceSchedule(String status, RentalListCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Sesi berakhir, silakan login kembali.");
            return;
        }

        String sellerId = user.getUid();

        firestore.collection("rentals")
                .whereEqualTo("sellerId", sellerId)
                .whereIn("status", Arrays.asList(status, "PROSES PERBAIKAN", "Komplain", "Menunggu Konfirmasi"))
                .orderBy("startDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError("Gagal memuat jadwal: " + error.getMessage());
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

    public void submitMaintenanceLog(Rental rental, Uri imageUri, String description, ActionCallback callback) {
        if (imageUri == null) {
            callback.onError("Foto laporan tidak boleh kosong!");
            return;
        }

        com.cloudinary.android.MediaManager.get().upload(imageUri).callback(new com.cloudinary.android.callback.UploadCallback() {
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                saveLogToFirestore(rental, imageUrl, description, callback);
            }
            @Override public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                callback.onError("Gagal unggah foto: " + error.getDescription());
            }
            @Override public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {}
        }).dispatch();
    }

    private void saveLogToFirestore(Rental rental, String imageUrl, String description, ActionCallback callback) {
        if ("PROSES PERBAIKAN".equalsIgnoreCase(rental.getStatus()) || "Komplain".equalsIgnoreCase(rental.getStatus())) {

            firestore.collection("rentals").document(rental.getRentalId())
                    .collection("complaints")
                    .whereIn("status", Arrays.asList("Pending", "PROSES PERBAIKAN"))
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        WriteBatch batch = firestore.batch();
                        DocumentReference logRef = firestore.collection("rentals").document(rental.getRentalId()).collection("maintenance_logs").document();
                        Map<String, Object> logData = new HashMap<>();
                        logData.put("logId", logRef.getId());
                        logData.put("description", "RESOLUSI KOMPLAIN: " + description);
                        logData.put("imageUrl", imageUrl);
                        logData.put("createdAt", FieldValue.serverTimestamp());
                        batch.set(logRef, logData);

                        // B. Update status Rental
                        DocumentReference rentalRef = firestore.collection("rentals").document(rental.getRentalId());
                        batch.update(rentalRef, "lastMaintenanceDate", FieldValue.serverTimestamp(), "status", "Menunggu Konfirmasi");

                        // C. Update Komplain (Selesaikan tiket)
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            batch.update(doc.getReference(),
                                    "status", "Menunggu Konfirmasi",
                                    "sellerResponseText",
                                    "Florist telah melakukan perbaikan di lokasi. Laporan: " + description,
                                    "sellerImageUrl", imageUrl,
                                    "respondedAt", FieldValue.serverTimestamp());
                        }

                        batch.commit()
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onError("Gagal menyimpan perbaikan: " + e.getMessage()));
                    })
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));

        } else {
            WriteBatch batch = firestore.batch();
            DocumentReference logRef = firestore.collection("rentals").document(rental.getRentalId()).collection("maintenance_logs").document();
            Map<String, Object> logData = new HashMap<>();
            logData.put("logId", logRef.getId());
            logData.put("description", description);
            logData.put("imageUrl", imageUrl);
            logData.put("createdAt", FieldValue.serverTimestamp());

            batch.set(logRef, logData);

            DocumentReference rentalRef = firestore.collection("rentals").document(rental.getRentalId());
            batch.update(rentalRef, "lastMaintenanceDate", FieldValue.serverTimestamp());

            batch.commit()
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onError("Gagal menyimpan laporan: " + e.getMessage()));
        }
    }
}