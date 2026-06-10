package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;

public class SplashViewModel extends ViewModel {

    private final MutableLiveData<String> userRole = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<String> getUserRole() {
        return userRole;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void fetchUserRole(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        userRole.setValue(role);
                    } else {
                        errorMessage.setValue("Data pengguna tidak ditemukan");
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue(e.getMessage());
                });
    }
}