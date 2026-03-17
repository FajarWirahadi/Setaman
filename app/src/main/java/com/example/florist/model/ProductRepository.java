package com.example.florist.model;

import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.Context;

public class ProductRepository {
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
//    StorageReference storageReference = FirebaseStorage.getInstance().getReference();
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    public interface ProductCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ProductCountCallback {
        void onSuccess(int count);
        void onError(String message);
    }

    public interface ProductListcallback {
        void onSuccess(List<Product> product);
        void onError(String message);
    }

    public void addProduct(Product product, Uri imageUri, ProductCallback callback) {
        String userId = FirebaseAuth.getInstance().getUid();

        String fileName = UUID.randomUUID().toString();

        MediaManager.get()
                .upload(imageUri)
                .callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {

            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {

            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String)  resultData.get("secure_url");
                product.setImageUrl(imageUrl);
                product.setOwnerId(userId);
                DocumentReference newProductRef = firestore.collection("products").document();
                product.setProductId(newProductRef.getId());

                newProductRef.set(product)
                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                        .addOnFailureListener(e -> callback.onError("Gagal simpan database: " + e.getMessage()));
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                callback.onError("Gagal upload gambar: " + error.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {

            }
        }).dispatch();
    }

    public void updateProduct(Product product, Uri imageUri, ProductCallback callback) {
        if (imageUri != null) {
            MediaManager.get()
                    .upload(imageUri)
                    .unsigned("setaman_upload")
                    .callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) {}
                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {}
                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String imageUrl = (String) resultData.get("secure_url");
                    product.setImageUrl(imageUrl);
                    saveProductToFirestore(product, callback);
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    callback.onError("Gagal update produk: " + error.getDescription());
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {

                }
            }).dispatch();
        } else {
            saveProductToFirestore(product, callback);
        }
    }

    private void saveProductToFirestore(Product product, ProductCallback productCallback) {
        firestore.collection("products").document(product.getProductId())
                .set(product)
                .addOnSuccessListener(aVoid -> {productCallback.onSuccess();})
                .addOnFailureListener(e -> productCallback.onError("Gagal update database: " + e.getMessage()));
    }

    // Di dalam ProductRepository.java

    public void updateProductWithMultipleImages(Product product, List<Uri> newImages, List<String> oldImages, ProductCallback callback) {
        if (newImages == null || newImages.isEmpty()) {
            product.setGallery(oldImages);
            if(!oldImages.isEmpty()) {
                product.setImageUrl(oldImages.get(0));
            } else {
                product.setImageUrl(null);
            }
            saveProductToFirestore(product, callback);
            return;
        }

        List<String> newCloudinaryUrls = new ArrayList<>();
        // AtomicInteger digunakan untuk menghitung jumlah upload yg selesai secara mana (thread safe)
        AtomicInteger uploadCounter = new AtomicInteger(0);
        int totalImagesToUpload = newImages.size();

        final boolean[] isError = {false};

        for (Uri imageUri : newImages) {
            MediaManager.get().upload(imageUri)
                    .unsigned("setaman_upload")
                    .callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) {

                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {

                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String secureUrl = (String) resultData.get("secure_url");

                    synchronized (newCloudinaryUrls) {
                        newCloudinaryUrls.add(secureUrl);
                    }
                    checkIsAllFinished();
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    isError[0] = true;
                    checkIsAllFinished();
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {

                }
                private void checkIsAllFinished() {
                    int currentCount = uploadCounter.incrementAndGet();

                    if (currentCount == totalImagesToUpload) {
                        if (isError[0]) {
                            callback.onError("Gagal mengupload beberapa gambar.");
                        } else {
                            List<String> finalGallery = new ArrayList<>();
                            if (oldImages!=null) finalGallery.addAll(oldImages);

                            // Perlu synchronized saat membaca hasil upload
                            synchronized (newCloudinaryUrls){
                                finalGallery.addAll(newCloudinaryUrls);
                            }
                            product.setGallery(finalGallery);

                            if (!finalGallery.isEmpty()) {
                                product.setImageUrl(finalGallery.get(0));
                            } else {
                                product.setImageUrl(null);
                            }
                            saveProductToFirestore(product, callback);
                        }
                    }
                }
            }).dispatch();
        }
    }

    public void updateProductStatus(String productId, boolean newStatus, ProductCallback callback) {
        firestore.collection("products").document(productId)
                .update("active", newStatus)
                .addOnSuccessListener(aVoid -> {callback.onSuccess();})
                .addOnFailureListener(e -> {callback.onError("Gagal mengubah status: " + e.getMessage());});
    }

    public void deleteProduct(String productId, ProductCallback callback) {
        firestore.collection("products").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError("Gagal menghapus produk: " + e.getMessage()));
    }

    public void getProductCountByOwner(String ownerId, ProductCountCallback callback) {
        firestore.collection("products")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    callback.onSuccess(count);
                })
                .addOnFailureListener(e -> callback.onError("Gagal menghitung jumlah product " + e.getMessage()));
    }

    public void getProductsByOwner(String ownerId, ProductListcallback callback) {
        firestore.collection("products")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> productList = queryDocumentSnapshots.toObjects(Product.class);
                    callback.onSuccess(productList);
                })
                .addOnFailureListener(e -> callback.onError("Gagal mengambil data produk: " + e.getMessage()));
    }
}
