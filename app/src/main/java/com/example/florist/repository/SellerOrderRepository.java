package com.example.florist.repository;

import com.example.florist.model.Order;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
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
                                order.setOrderId(doc.getId());
                                orderList.add(order);
                            }
                        }
                        callback.onSuccess(orderList);
                    }
                });
    }

    public void updateOrderStatus(String orderId, String newStatus, ActionCallback callback) {
        // 1. Cari semua tanaman (rentals) yang tergabung dalam pesanan ini
        firestore.collection("rentals").whereEqualTo("orderId", orderId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = firestore.batch();

                    // 2. Update status invoice utama (orders)
                    DocumentReference orderRef = firestore.collection("orders").document(orderId);
                    batch.update(orderRef, "status", newStatus);

                    // 3. Konversi status order menjadi status rental
                    // Jika pesanan Diproses/Dikirim, maka perawatannya otomatis AKTIF
                    String rentalStatus = newStatus;
                    if (newStatus.equals("Diproses") || newStatus.equals("Dikirim")) {
                        rentalStatus = "AKTIF";
                    } else if (newStatus.equals("Selesai")) {
                        rentalStatus = "SELESAI";
                    }

                    // 4. Timpa status semua tanaman tersebut serentak
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "status", rentalStatus);
                    }

                    // 5. Eksekusi mati!
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void rejectOrder(String orderId, String reason, ActionCallback callback) {
        firestore.collection("rentals").whereEqualTo("orderId", orderId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = firestore.batch();

                    DocumentReference orderRef = firestore.collection("orders").document(orderId);
                    batch.update(orderRef,
                            "status", "Dibatalkan",
                            "cancellationReason", reason
                    );

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(),
                                "status", "Dibatalkan",
                                "cancellationReason", reason
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}