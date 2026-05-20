package com.example.florist.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SellerOrderRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface OrderListCallback  {
        void onSuccess(List<Order> orders);
        void onError(String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public ListenerRegistration listenToSellerOrder (String sellerId, String status, OrderListCallback callback) {
        return firestore.collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        List<Order> orderList = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                orderList.add(order);
                            }
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

    public void rejectOrder(String orderId, String reason, ActionCallback callback) {
        firestore.collection("orders").document(orderId)
                .update(
                        "status", "Dibatalkan",
                        "cancellationReason", reason
                )
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addMaintenanceLog(String orderId, MaintenanceLog log, ActionCallback callback) {
        firestore.collection("orders").document(orderId)
                .collection("maintenance_logs").document(log.getLogId())
                .set(log)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
