package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.DeliveryLog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BuyerTrackingViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<DeliveryLog>> trackingLogs = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<List<DeliveryLog>> getTrackingLogs() { return trackingLogs; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void fetchTrackingHistory(String orderId) {
        db.collection("orders").document(orderId)
                .collection("delivery_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        errorMessage.setValue("Gagal memuat pelacakan: " + error.getMessage());
                        return;
                    }

                    List<DeliveryLog> logs = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            DeliveryLog log = doc.toObject(DeliveryLog.class);
                            logs.add(log);
                        }
                    }
                    trackingLogs.setValue(logs);
                });
    }
}