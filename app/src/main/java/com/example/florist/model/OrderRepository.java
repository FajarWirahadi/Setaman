package com.example.florist.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class OrderRepository {
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface OrderListCallback {
        void onSuccess (List<Order> orders);
        void onError(String errorMessage);
    }

    public void getOrderByStatus(String buyerId, String status, OrderListCallback callback) {
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
                            orderList.add(order);
                        }
                        callback.onSuccess(orderList);
                    }
                });
    }
}
