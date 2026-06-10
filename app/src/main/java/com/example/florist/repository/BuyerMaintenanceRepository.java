package com.example.florist.repository;

import com.example.florist.model.MaintenanceLog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class BuyerMaintenanceRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface MaintenanceLogCallback {
        void onSuccess(List<MaintenanceLog> logs);
        void onError(String message);
    }

    public ListenerRegistration listenToMaintenanceLogs(String rentalId, MaintenanceLogCallback callback) {

        return firestore.collection("rentals")
                .document(rentalId)
                .collection("maintenance_logs")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<MaintenanceLog> logList = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            MaintenanceLog log = doc.toObject(MaintenanceLog.class);
                            if (log != null) {
                                logList.add(log);
                            }
                        }
                        callback.onSuccess(logList);
                    }
                });
    }
}