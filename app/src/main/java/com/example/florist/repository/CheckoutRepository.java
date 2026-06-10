package com.example.florist.repository;

import com.example.florist.model.CartItem;
import com.example.florist.model.DeliveryAddress;
import com.example.florist.model.Order;
import com.example.florist.model.Rental;
import com.example.florist.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String error);
    }

    public interface ActionCallback {
        void onSuccess(String orderId);

        void onError(String error);
    }

    public void getUserProfile(String userId, DataCallback<User> callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSuccess(doc.toObject(User.class));
                    } else {
                        callback.onError("User tidak ditemukan");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getMainAddress(String userId, DataCallback<DeliveryAddress> callback) {
        db.collection("users").document(userId).collection("addresses")
                .whereEqualTo("mainAddress", true).limit(1).get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        callback.onSuccess(query.getDocuments().get(0).toObject(DeliveryAddress.class));
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getCartItems(String userId, DataCallback<List<CartItem>> callback) {
        db.collection("users").document(userId).collection("cart").get()
                .addOnSuccessListener(query -> {
                    List<CartItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        CartItem item = doc.toObject(CartItem.class);
                        if (item != null) items.add(item);
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void processCheckout(List<CartItem> cartItems, String buyerId, String buyerName, DeliveryAddress deliveryAddress, String paymentMethod, boolean isDirectBuy, ActionCallback callback) {

        Map<String, List<CartItem>> groupedOrders = new HashMap<>();

        for (CartItem item : cartItems) {
            String sellerId = item.getOwnerId();
            if (!groupedOrders.containsKey(sellerId)) {
                groupedOrders.put(sellerId, new ArrayList<>());
            }
            groupedOrders.get(sellerId).add(item);
        }

        WriteBatch batch = db.batch();
        DocumentReference orderRef = db.collection("orders").document();

        for (Map.Entry<String, List<CartItem>> entry : groupedOrders.entrySet()) {
            String sellerId = entry.getKey();
            List<CartItem> itemsForThisSeller = entry.getValue();

            double totalAmount = 0;
            for (CartItem item : itemsForThisSeller) {
                long duration = item.getDurationValue() > 0 ? item.getDurationValue() : 1;
                totalAmount += (item.getPrice() * item.getQuantity() * duration);
            }

            Order newOrder = new Order(orderRef.getId(), buyerId, sellerId, itemsForThisSeller, totalAmount, deliveryAddress, paymentMethod);
            batch.set(orderRef, newOrder);

            for (CartItem item : itemsForThisSeller) {
                DocumentReference rentalRef = db.collection("rentals").document();

                Rental newRental = new Rental();
                newRental.setRentalId(rentalRef.getId());
                newRental.setOrderId(orderRef.getId());
                newRental.setBuyerId(buyerId);
                newRental.setSellerId(sellerId);
                newRental.setSellerName(item.getShopName());

                newRental.setBuyerName(buyerName);
                newRental.setReceiverName(deliveryAddress != null ? deliveryAddress.getReceiverName() : buyerName);
                newRental.setDeliveryAddress(deliveryAddress);
                newRental.setPlantName(item.getName());
                newRental.setPlantImageUrl(item.getImageUrl());
                long durationVal = item.getDurationValue() > 0 ? item.getDurationValue() : 1;
                newRental.setTotalAmount(item.getPrice() * item.getQuantity() * durationVal);
                newRental.setStatus("Pending");

                long days = 0;
                String durType = item.getDurationType();
                if (durType != null) {
                    if (durType.toLowerCase().contains("hari")) {
                        days = item.getDurationValue();
                    } else if (durType.toLowerCase().contains("minggu")) {
                        days = item.getDurationValue() * 7L;
                    } else if (durType.toLowerCase().contains("bulan")) {
                        days = item.getDurationValue() * 30L;
                    }
                }

                long durationInMillis = days * 24L * 60L * 60L * 1000L;
                long now = System.currentTimeMillis();

                newRental.setStartDate(new com.google.firebase.Timestamp(new java.util.Date(now)));

                if (durationInMillis > 0) {
                    newRental.setEndDate(new com.google.firebase.Timestamp(new java.util.Date(now + durationInMillis)));
                } else {
                    newRental.setEndDate(new com.google.firebase.Timestamp(new java.util.Date(now)));
                }

                batch.set(rentalRef, newRental);
            }
        }

        // HAPUS KERANJANG
        if (!isDirectBuy) {
            for (CartItem item : cartItems) {
                DocumentReference cartRef = db.collection("users").document(buyerId).collection("cart").document(item.getProductId());
                batch.delete(cartRef);
            }
        }

        String finalOrderId = orderRef.getId();

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(finalOrderId))
                .addOnFailureListener(e -> callback.onError("Gagal checkout: " + e.getMessage()));
    }
}