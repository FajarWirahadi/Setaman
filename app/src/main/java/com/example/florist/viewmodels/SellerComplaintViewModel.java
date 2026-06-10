package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.repository.AuthRepository;
import com.example.florist.model.Rental; // KITA PAKAI RENTAL SEKARANG
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class SellerComplaintViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AuthRepository authRepository = AuthRepository.getInstance();
    private final MutableLiveData<List<Rental>> complaintRentals = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSessionExpired = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration;

    public LiveData<List<Rental>> getComplaintRentals() { return complaintRentals; }
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

        // BACA DARI KOLEKSI RENTALS, BUKAN ORDERS!
        listenerRegistration = db.collection("rentals")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("hasComplaint", true)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);

                    if (error != null) {
                        errorMessage.setValue("Gagal memuat komplain: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<Rental> rentals = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value) {
                            Rental rental = doc.toObject(Rental.class);
                            if (rental != null) {
                                rentals.add(rental);
                            }
                        }
                        complaintRentals.setValue(rentals);
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