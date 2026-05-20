package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Complaint;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

public class SellerComplaintDetailViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<Complaint> activeComplaint = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isResolveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration;

    public LiveData<Complaint> getActiveComplaint() { return activeComplaint; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsResolveSuccess() { return isResolveSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void fetchComplaintDetail(String orderId) {
        isLoading.setValue(true);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        listenerRegistration = db.collection("orders")
                .document(orderId)
                .collection("complaints")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);

                    if (error != null) {
                        errorMessage.setValue("Gagal memuat detail komplain: " + error.getMessage());
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        Complaint complaint = value.getDocuments().get(0).toObject(Complaint.class);
                        activeComplaint.setValue(complaint);
                    } else {
                        errorMessage.setValue("Data komplain tidak ditemukan.");
                    }
                });
    }
    
    public void resolveComplaint(String orderId, String complaintId, String responseText) {
        isLoading.setValue(true);

        WriteBatch batch = db.batch();

        DocumentReference complaintRef = db.collection("orders")
                .document(orderId)
                .collection("complaints")
                .document(complaintId);

        batch.update(complaintRef,
                "sellerResponseText", responseText,
                "status", "Resolved",
                "resolvedAt", Timestamp.now());

        DocumentReference orderRef = db.collection("orders").document(orderId);
        batch.update(orderRef, "status", "Dalam Perawatan");

        batch.commit().addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                isResolveSuccess.setValue(true);
            } else {
                errorMessage.setValue("Gagal mengirim resolusi: " + task.getException().getMessage());
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