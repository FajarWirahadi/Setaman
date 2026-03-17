package com.example.florist.model;

import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Map;

public class ShopRepository {
    private FirebaseFirestore firestore;
    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;

    public ShopRepository() {
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
    }

    public interface ShopCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ShopDataCallback {
        void onDataLoaded(Shop shop);
        void onError(String message);
    }

    public interface UpdateImageCallback {
        void onSuccess(String newImageUrl);
        void onError(String message);
    }

    public void createShop (String name, String address, Uri imageUri, ShopCallback callback) {
        String userId = firebaseAuth.getCurrentUser().getUid();

        if (imageUri != null) {
            MediaManager.get().upload(imageUri).callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) {

                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {

                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String imageUrl = (String) resultData.get("secure_url");
                    Shop newShop = new Shop(userId, name, address,imageUrl);
                    saveShopDataBatch(newShop, userId, callback);
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    callback.onError("Gagal upload toko: " + error.getDescription());
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {

                }
            }).dispatch();
        } else {
            Shop newShop = new Shop(userId, name, address, null);
            saveShopDataBatch(newShop, userId, callback);
        }


    }

    private void saveShopDataBatch(Shop shop, String userId, ShopCallback callback) {
        WriteBatch batch = firestore.batch();

        DocumentReference shopRef = firestore.collection("shops").document(userId);
        batch.set(shopRef, shop);

        DocumentReference userRef = firestore.collection("users").document(userId);
        batch.update(userRef, "hasShop", true);
        batch.update(userRef, "shopId", userId);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError("Gagal menyimpan data toko: " + e.getMessage()));
    }

    public void getShopData(String shopId, ShopDataCallback callback) {
        firestore.collection("shops").document(shopId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Shop shop = documentSnapshot.toObject(Shop.class);
                        callback.onDataLoaded(shop);
                    } else {
                        callback.onError("Data toko tidak ditemukan");
                    }
                })
                .addOnFailureListener(e -> callback.onError("Gagal mengambil data toko: " + e.getMessage()));
    }

    public void updateShopImage(Uri imageUri, UpdateImageCallback callback) {
        String userId = firebaseAuth.getCurrentUser().getUid();

        Log.d("DEBUG_UPLOAD", "1. Memulai upload ke Cloudinary. URI: " + imageUri.toString());

        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                Log.d("DEBUG_UPLOAD", "2. onStart: Cloudinary mulai memproses request...");
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                Log.d("DEBUG_UPLOAD", "3. onProgress: Mengunggah... " + bytes + " dari " + totalBytes + " bytes");
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                Log.d("DEBUG_UPLOAD", "4. onSuccess: Gambar masuk ke Cloudinary! URL: " + imageUrl);

                Log.d("DEBUG_UPLOAD", "5. Menyimpan URL ke Firestore...");
                firestore.collection("shops").document(userId)
                        .update("shopImageUrl", imageUrl)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("DEBUG_UPLOAD", "6. SUKSES TOTAL! Data Firestore terupdate.");
                            callback.onSuccess(imageUrl);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("DEBUG_UPLOAD", "GAGAL FIRESTORE: " + e.getMessage());
                            callback.onError("Gagal update database: " + e.getMessage());
                        });
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Log.e("DEBUG_UPLOAD", "GAGAL CLOUDINARY: " + error.getDescription());
                callback.onError("Gagal upload gambar: " + error.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                Log.w("DEBUG_UPLOAD", "RE-SCHEDULE: Proses diulang karena error: " + error.getDescription());
            }
        }).dispatch();
    }


}


