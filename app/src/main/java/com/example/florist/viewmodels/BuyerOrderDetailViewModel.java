package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Order;
import com.google.firebase.firestore.FirebaseFirestore;

public class BuyerOrderDetailViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<Order> orderDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<Order> getOrderDetail() { return orderDetail; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void fetchOrderDetail(String orderId) {
        isLoading.setValue(true);

        db.collection("orders").document(orderId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    isLoading.setValue(false);
                    if (documentSnapshot.exists()) {
                        Order order = documentSnapshot.toObject(Order.class);
                        orderDetail.setValue(order);
                    } else {
                        errorMessage.setValue("Pesanan tidak ditemukan di database.");
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Gagal memuat data: " + e.getMessage());
                });
    }
}