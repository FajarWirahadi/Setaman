package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Order;
import com.example.florist.repository.OrderRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ShoppingViewModel extends ViewModel {
    private final OrderRepository repository = new OrderRepository();
    private ListenerRegistration registration;

    private List<Order> allOrders = new ArrayList<>();

    private final MutableLiveData<List<Order>> filteredOrders = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionSuccessMessage = new MutableLiveData<>();

    private String currentFilter = "SEMUA";

    public LiveData<List<Order>> getFilteredOrders() { return filteredOrders; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getActionSuccessMessage() { return actionSuccessMessage; }


    public void fetchOrders(String buyerId) {
        isLoading.setValue(true);

        registration = repository.getAllBuyerOrders(buyerId, new OrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                isLoading.setValue(false);
                allOrders = orders;
                applyCurrentFilter();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void setFilter(String status) {
        this.currentFilter = status;
        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        if ("SEMUA".equalsIgnoreCase(currentFilter)) {
            filteredOrders.setValue(allOrders);
            return;
        }

        List<Order> filteredList = new ArrayList<>();
        for (Order order : allOrders) {
            if (currentFilter.equalsIgnoreCase(order.getStatus())) {
                filteredList.add(order);
            }
        }
        filteredOrders.setValue(filteredList);
    }

    public void loadMyOrders() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            fetchOrders(currentUser.getUid());
        } else {
            isLoading.setValue(false);
            errorMessage.setValue("Harap login terlebih dahulu untuk melihat pesanan.");
        }
    }

    public void acceptOrder(com.example.florist.model.Order order) {
        isLoading.setValue(true);
        new com.example.florist.repository.OrderRepository().acceptDeliveredOrder(order, new com.example.florist.repository.OrderRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                actionSuccessMessage.setValue("Pesanan berhasil diterima!");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal menerima pesanan: " + message);
            }
        });
    }

    public void endRental(com.example.florist.model.Order order) {
        isLoading.setValue(true);
        new com.example.florist.repository.OrderRepository().updateOrderStatus(order.getOrderId(), "Selesai", new com.example.florist.repository.OrderRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                actionSuccessMessage.setValue("Masa sewa selesai. Terima kasih!");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal mengakhiri sewa: " + message);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (registration != null) {
            registration.remove();
        }
    }
}