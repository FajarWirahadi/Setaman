package com.example.florist.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.florist.model.Complaint;
import com.example.florist.model.ComplaintRepository;
import com.google.firebase.Timestamp;

import java.util.Map;
import java.util.UUID;

public class ComplaintViewModel extends ViewModel {
    private final ComplaintRepository repository = new ComplaintRepository();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<Boolean> getIsSuccess() {return isSuccess;}
    public LiveData<String> getErrorMessage() {return errorMessage;}

    public void submitComplaint(String orderId, String reason, String desc, Uri imageUri) {
        isLoading.setValue(true);

        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                String complaintId = UUID.randomUUID().toString();
                Complaint complaint = new Complaint(
                        complaintId, reason, desc, imageUrl, "Pending", Timestamp.now()
                );

                repository.submitComplaint(orderId, complaint, new ComplaintRepository.OnActionCallback() {
                    @Override
                    public void onSuccess() {
                        isLoading.setValue(false);
                        isSuccess.setValue(true);
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
                errorMessage.setValue("Gagal mengunggah foto: " + error.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                isLoading.setValue(false);
                errorMessage.setValue("Upload ditunda karena masalah jaringan.");
            }
        }).dispatch();
    }
}
