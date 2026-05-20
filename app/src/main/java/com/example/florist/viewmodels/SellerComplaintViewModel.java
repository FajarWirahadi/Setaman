package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.AuthRepository;
import com.example.florist.model.Order;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class SellerComplaintViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AuthRepository authRepository = AuthRepository.getInstance();

    private final MutableLiveData<List<Order>> complaintOrders = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSessionExpired = new MutableLiveData<>(); // LiveData Sesi

    private ListenerRegistration listenerRegistration;

    public LiveData<List<Order>> getComplaintOrders() { return complaintOrders; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsSessionExpired() { return isSessionExpired; }

    public void fetchComplaintOrders() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            isSessionExpired.setValue(true);
            return;
        }

        String sellerId = user.getUid();
        isLoading.setValue(true);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        listenerRegistration = db.collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("status", "Komplain")
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);

                    if (error != null) {
                        errorMessage.setValue("Gagal memuat komplain: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<Order> orders = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                orders.add(order);
                            }
                        }
                        complaintOrders.setValue(orders);
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}