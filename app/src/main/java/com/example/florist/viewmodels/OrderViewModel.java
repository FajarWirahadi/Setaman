package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Order;
import com.example.florist.model.OrderRepository;

import java.util.List;

public class OrderViewModel extends ViewModel {
    private OrderRepository repository;

    private final MutableLiveData<List<Order>> orderList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public OrderViewModel() {
        repository = new OrderRepository();
    }

    public LiveData<List<Order>> getOrderList() {return orderList;}
    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<String> getErrorMessage() {return errorMessage;}

    public void fetchOrders(String buyerId, String status) {
        isLoading.setValue(true);
        repository.getOrderByStatus(buyerId, status, new OrderRepository.OrderListCallback() {
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
}
