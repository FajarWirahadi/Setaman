package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.DeliveryLog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class SellerDeliveryViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsSuccess() { return isSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void addDeliveryLog(String orderId, String statusTitle, String note) {
        isLoading.setValue(true);

        DocumentReference orderRef = db.collection("orders").document(orderId);
        DocumentReference newLogRef = orderRef.collection("delivery_logs").document();

        DeliveryLog newLog = new DeliveryLog(
                newLogRef.getId(),
                statusTitle,
                note,
                Timestamp.now()
        );

        WriteBatch batch = db.batch();

        batch.set(newLogRef, newLog);

        if (statusTitle.contains("Pesanan Diterima Pembeli")) {
            batch.update(orderRef, "status", "Dalam Perawatan");
        }

        batch.commit().addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                isSuccess.setValue(true);
            } else {
                errorMessage.setValue("Gagal memperbarui status: " + task.getException().getMessage());
            }
        });
    }
}