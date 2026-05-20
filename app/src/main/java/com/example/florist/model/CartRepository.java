package com.example.florist.model;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CartRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface CartListCallback {
        void onDataChange(List<CartItem> cartItems);
        void onError(String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface CountCallback {
        void onSuccess(int count);
        void onError(String errorMessage);
    }

    public void addToCart(String buyerId, CartItem item, ActionCallback callback) {
        firestore.collection("users").document(buyerId)
                .collection("cart").document(item.getProductId())
                .set(item)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getShopById (String shopId, ActionCallback callback) {
        firestore.collection("shops").document(shopId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Shop shop = documentSnapshot.toObject(Shop.class);
                        callback.onSuccess();
                    }})
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    public void getCartItemCount(String userId, CountCallback callback) {
        firestore.collection("users").document(userId)
                .collection("cart")
                .count()
                .get(AggregateSource.SERVER)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess((int) task.getResult().getCount());
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    public void listenToCartItems(String userId, CartListCallback callback) {
        firestore.collection("users").document(userId).collection("cart")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<CartItem> list = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            CartItem item = doc.toObject(CartItem.class);
                            if (item != null) list.add(item);
                        }
                    }
                    callback.onDataChange(list);
                });
    }

    // 2. Mengupdate field tertentu (Quantity, Duration)
    public void updateCartItem(String userId, String productId, String field, Object value, ActionCallback callback) {
        firestore.collection("users").document(userId)
                .collection("cart").document(productId)
                .update(field, value)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 3. Mengupdate banyak field sekaligus (Untuk Edit Durasi)
    public void updateCartItemMultiple(String userId, String productId, java.util.Map<String, Object> updates, ActionCallback callback) {
        firestore.collection("users").document(userId)
                .collection("cart").document(productId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 4. Menghapus item
    public void deleteCartItem(String userId, String productId, ActionCallback callback) {
        firestore.collection("users").document(userId)
                .collection("cart").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
