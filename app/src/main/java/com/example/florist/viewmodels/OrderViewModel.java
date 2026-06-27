package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Order;
import com.example.florist.repository.OrderRepository;

import java.util.List;

public class OrderViewModel extends ViewModel {
    private OrderRepository repository;

    private final MutableLiveData<List<Order>> orderList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionSuccessMessage = new MutableLiveData<>();

    public OrderViewModel() {
        repository = new OrderRepository();
    }

    public LiveData<List<Order>> getOrderList() {return orderList;}
    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<String> getErrorMessage() {return errorMessage;}
    public LiveData<String> getActionSuccessMessage() {return actionSuccessMessage;}

    public void fetchOrders(String status) {
        isLoading.setValue(true);
        repository.getOrderByStatus(status, new OrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                isLoading.setValue(false);
                orderList.setValue(orders);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void acceptOrder(Order order) {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;

        isLoading.setValue(true);
        repository.acceptDeliveredOrder(order, new OrderRepository.ActionCallback() {
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

    public void endRental(Order order) {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;

        isLoading.setValue(true);
        repository.updateOrderStatus(order.getOrderId(), "SELESAI", new OrderRepository.ActionCallback() {
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
}
