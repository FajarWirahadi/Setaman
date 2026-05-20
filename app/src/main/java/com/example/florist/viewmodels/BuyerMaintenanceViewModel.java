package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.BuyerMaintenanceRepository;
import com.example.florist.model.MaintenanceLog;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class BuyerMaintenanceViewModel extends ViewModel {
    private final BuyerMaintenanceRepository repository = new BuyerMaintenanceRepository();
    private final MutableLiveData<List<MaintenanceLog>> maintenanceLogs = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private ListenerRegistration registration;

    public LiveData<List<MaintenanceLog>> getMaintenanceLogs() { return maintenanceLogs; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void startListening(String orderId) {
        registration = repository.listenToMaintenanceLogs(orderId, new BuyerMaintenanceRepository.MaintenanceLogCallback() {
            @Override
            public void onSuccess(List<MaintenanceLog> logs) {
                maintenanceLogs.setValue(logs);
            }

            @Override
            public void onError(String message) {
                errorMessage.setValue(message);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (registration != null) registration.remove();
    }
}