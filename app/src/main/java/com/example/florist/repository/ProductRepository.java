package com.example.florist.repository;

import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.florist.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ProductRepository {
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    public static class ShopData {
        public String shopName;
        public String shopImageUrl;

        public ShopData(String shopName, String shopImageUrl) {
            this.shopName = shopName;
            this.shopImageUrl = shopImageUrl;
        }
    }


    public interface ProductCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ProductCountCallback {
        void onSuccess(int count);
        void onError(String message);
    }

    public interface ProductListCallback {
        void onSuccess(List<Product> product);
        void onError(String message);
    }

    public interface ShopNamesCallback {
        void onSuccess(HashMap<String, ShopData> shopNames);
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

    public void deleteProduct(Product product, ProductCallback callback) {
        if (product.getImageUrl() != null) {
            deleteImageFromCloudinary(product.getImageUrl());
        }

        if (product.getGallery() != null && !product.getGallery().isEmpty()) {
            for (String url: product.getGallery()) {
                deleteImageFromCloudinary(url);
            }
        }

        firestore.collection("products").document(product.getId())
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

    public void getProductsByOwner(String ownerId, ProductListCallback callback) {
        firestore.collection("products")
                .whereEqualTo("ownerId", ownerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError("Gagal mendapatkan data: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        List<Product> productList = value.toObjects(Product.class);
                        callback.onSuccess(productList);
                    }
                });
    }

    public void getAllProducts(ProductListCallback callback) {
        firestore.collection("products")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> productList = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Product product = doc.toObject(Product.class);
                        if (product != null) {
                            productList.add(product);
                        }
                    }
                    callback.onSuccess(productList);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void deleteImageFromCloudinary(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                java.net.URL url = new java.net.URL("http://192.168.1.55:3000/api/delete-image");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application.json");
                conn.setDoOutput(true);

                String jsonInputString = "{\"imageUrl\": \"" + imageUrl + "\"}";

                try(java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int responCode = conn.getResponseCode();
                android.util.Log.d("Setaman", "Respon harus gambar " + imageUrl + ": " + responCode);
            } catch (Exception e) {
                android.util.Log.e("Setaman", "Gagal menghubungi server", e);
            }
        });
    }

    public void getShopNames(ShopNamesCallback callback) {
        firestore.collection("shops").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.HashMap<String, ShopData> map = new java.util.HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String nameOfShop = doc.getString("shopName");
                        String imageUrl = doc.getString("shopImageUrl");

                        if (nameOfShop != null) {
                            map.put(doc.getId(), new ShopData(nameOfShop, imageUrl != null ? imageUrl : ""));
                        }
                    }
                    callback.onSuccess(map);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
