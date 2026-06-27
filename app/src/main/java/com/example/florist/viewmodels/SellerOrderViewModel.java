package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Order;
import com.example.florist.repository.AuthRepository;
import com.example.florist.repository.SellerOrderRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class SellerOrderViewModel extends ViewModel {
    private final SellerOrderRepository repository;
    private final AuthRepository authRepository;

    private final MutableLiveData<List<Order>> allSellerOrders = new MutableLiveData<List<Order>>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionSuccessMessage = new MutableLiveData<>();
    private ListenerRegistration orderListener;

    public SellerOrderViewModel() {
        repository = new SellerOrderRepository();
        authRepository = AuthRepository.getInstance();
    }

    public LiveData<List<Order>> getAllSellerOrders() {return allSellerOrders;}
    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<String> getErrorMessage() {return errorMessage;}
    public LiveData<String> getActionSuccessMessage() {return actionSuccessMessage;}

    public void fetchSellerOrders(String status) {
        String sellerId = FirebaseAuth.getInstance().getUid();
        if (sellerId == null) return;

        isLoading.setValue(true);
        if (orderListener != null) orderListener.remove();

        orderListener = repository.listenToSellerOrder(sellerId, status, new SellerOrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                isLoading.setValue(false);
                long currentTime = System.currentTimeMillis();

                for (Order order : orders) {
                    if ("MENUNGGU KONFIRMASI".equals(order.getStatus()) && order.getCreatedAt() != null) {
                        long createdAtTime = order.getCreatedAt().toDate().getTime();
                        long deadlineTime = createdAtTime + (24L * 60L * 60L * 1000L); // SLA 24 Jam
                        long remainingMillis = deadlineTime - currentTime;

                        if (remainingMillis > 0) {
                            long hours = remainingMillis / (1000 * 60 * 60);
                            long minutes = (remainingMillis % (1000 * 60 * 60)) / (1000 * 60);

                            order.setSlaText(String.format("Batas Respon: %dj %dm lagi", hours, minutes));
                            order.setSlaUrgent(hours < 3); // True jika masa tinggal kurang dari 3 jam
                        } else {
                            order.setSlaText("Waktu Respon Habis!");
                            order.setSlaUrgent(true);
                        }
                    }
                }
                allSellerOrders.setValue(orders);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void updateOrderStatus(Order order, String newStatus) {
        isLoading.setValue(true);
        repository.updateOrderStatus(order.getOrderId(), newStatus, new SellerOrderRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                actionSuccessMessage.setValue("Pesanan berhasil diubah menjadi " + newStatus);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal mengubah status: " + message);
            }
        });
    }

    public void rejectOrder(Order order, String reason) {
        isLoading.setValue(true);
        repository.rejectOrder(order.getOrderId(), reason, new SellerOrderRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                actionSuccessMessage.setValue("Pesanan berhasil ditolak.");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal menolak pesanan: " +  message);
            }
        });
    }

    @Override
    protected void onCleared() {
        if (orderListener != null) {
            orderListener.remove();
        }
    }
}
