package com.example.florist.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.florist.model.AuthRepository;
import com.example.florist.model.MaintenanceLog;
import com.example.florist.model.Order;
import com.example.florist.model.SellerOrderRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MaintenanceViewModel extends ViewModel {
    private final SellerOrderRepository repository;
    private final AuthRepository authRepository;
    private MutableLiveData<List<Order>> orderList = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionSuccessMessage = new MutableLiveData<>();

    public MaintenanceViewModel() {
        repository = new SellerOrderRepository();
        authRepository = AuthRepository.getInstance();
    }

    public LiveData<List<Order>> getOrdersInMaintenance() {return orderList;}
    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<String> getErrorMessage() {return errorMessage;}
    public LiveData<String> getActionSuccessMessage() {return actionSuccessMessage;}

    public void AddMaintenance(Order order, Uri imageUri, String description) {
        isLoading.setValue(true);

        String fileName = UUID.randomUUID().toString();
        MediaManager
                .get()
                .upload(imageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {

                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {

                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String)  resultData.get("secure_url");

                        MaintenanceLog maintenanceLog = new MaintenanceLog(
                                fileName,
                                description,
                                imageUrl,
                                Timestamp.now());

                        repository.addMaintenanceLog(order.getOrderId(), maintenanceLog, new SellerOrderRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                isLoading.setValue(false);
                                actionSuccessMessage.setValue("Laporan perawatan tanaman berhasil dikirim");

                            }

                            @Override
                            public void onError(String message) {
                                isLoading.setValue(false);
                                errorMessage.setValue(message);
                            }
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Gagal menggungah foto: " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Upload ditunda karena masalah jaringan.");
                    }
                }).dispatch();


    }

    public void fetchOrdersInMaintenance() {
        String sellerId = authRepository.getCurrentUser().getUid();
        if (sellerId  == null) return;

        isLoading.setValue(true);
        repository.listenToSellerOrder(sellerId, "Dalam Perawatan", new SellerOrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                isLoading.setValue(false);
                orderList.setValue(orders);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

}
