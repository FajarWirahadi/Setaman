package com.example.florist.repository;

import com.example.florist.model.Order;
import com.example.florist.model.Rental;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class OrderRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface OrderListCallback {
        void onSuccess(List<Order> orders);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public void getOrderByStatus(String status, OrderListCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError("Sesi berakhir, silakan login kembali.");
            return;
        }

        String buyerId = user.getUid();
        firestore.collection("orders")
                .whereEqualTo("buyerId", buyerId)
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError("Gagal memuat pesanan: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<Order> orderList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Order order = doc.toObject(Order.class);
                            if (order.getOrderId() == null) order.setOrderId(doc.getId());
                            orderList.add(order);
                        }
                        callback.onSuccess(orderList);
                    }
                });
    }

    public ListenerRegistration getAllBuyerOrders(String buyerId, OrderListCallback callback) {
        return firestore.collection("orders")
                .whereEqualTo("buyerId", buyerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError("Gagal memuat pesanan: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<Order> orderList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Order order = doc.toObject(Order.class);
                            if (order.getOrderId() == null) order.setOrderId(doc.getId());
                            orderList.add(order);
                        }
                        callback.onSuccess(orderList);
                    }
                });
    }

    public void updateOrderStatus(String orderId, String newStatus, ActionCallback callback) {
        firestore.collection("orders").document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void acceptDeliveredOrder(Order order, ActionCallback callback) {
        firestore.collection("rentals").whereEqualTo("orderId", order.getOrderId()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = firestore.batch();
                    DocumentReference orderRef = firestore.collection("orders").document(order.getOrderId());
                    batch.update(orderRef, "status", "MENUNGGU KONFIRMASI");
                    batch.update(orderRef, "orderType", "SEWA");

                    long now = System.currentTimeMillis();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Rental rentalItem = doc.toObject(Rental.class);

                        if (rentalItem.getStartDate() != null && rentalItem.getEndDate() != null) {
                            long originalDuration = rentalItem.getEndDate().toDate().getTime() - rentalItem.getStartDate().toDate().getTime();
                            batch.update(doc.getReference(), "startDate", new Timestamp(new java.util.Date(now)));
                            batch.update(doc.getReference(), "endDate", new Timestamp(new java.util.Date(now + originalDuration)));
                        }

                        batch.update(doc.getReference(), "status", "SEWA AKTIF");
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError("Gagal memperbarui pesanan: " + e.getMessage()));

                })
                .addOnFailureListener(e -> callback.onError("Gagal mengambil data sewa: " + e.getMessage()));
    }
}