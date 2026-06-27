package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.repository.AuthRepository;
import com.example.florist.model.Complaint; // KITA PAKAI COMPLAINT SEKARANG
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SellerComplaintViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AuthRepository authRepository = AuthRepository.getInstance();

    // GANTI TIPE LIST MENJADI COMPLAINT
    private final MutableLiveData<List<Complaint>> complaintList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSessionExpired = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration;

    public LiveData<List<Complaint>> getComplaintList() { return complaintList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsSessionExpired() { return isSessionExpired; }

    public void fetchComplaintOrders() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            isSessionExpired.setValue(true);
            return;
        }

        String sellerId = user.getUid();
        isLoading.setValue(true);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        // QUERY LANGSUNG KE TIKET KOMPLAIN YANG STATUSNYA BUKAN SELESAI
        listenerRegistration = db.collection("complaints")
                .whereEqualTo("sellerId", sellerId)
                .whereNotEqualTo("status", "SELESAI")
                .orderBy("status")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);

                    if (error != null) {
                        errorMessage.setValue("Gagal memuat tiket komplain: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<Complaint> complaints = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value) {
                            Complaint complaint = doc.toObject(Complaint.class);
                            if (complaint != null) {
                                complaints.add(complaint);
                            }
                        }
                        complaintList.setValue(complaints);
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