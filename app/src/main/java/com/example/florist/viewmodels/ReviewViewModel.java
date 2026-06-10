package com.example.florist.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Review;
import com.example.florist.repository.ReviewRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ReviewViewModel extends ViewModel {

    private final ReviewRepository repository = new ReviewRepository();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsSuccess() { return isSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void submitReview(String orderId, String productId, float rating, String comment, Uri imageUri) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            errorMessage.setValue("Sesi berakhir. Silakan login kembali.");
            return;
        }

        isLoading.setValue(true);
        String buyerName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Pembeli";

        Review newReview = new Review(
                "",
                orderId,
                productId,
                currentUser.getUid(),
                buyerName,
                rating,
                comment,
                ""
        );

        // Jika user memilih gambar, upload ke Cloudinary dulu
        if (imageUri != null) {
            repository.uploadReviewImage(imageUri, new ReviewRepository.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    newReview.setImageUrl(imageUrl);
                    saveToDatabase(newReview);
                }

                @Override
                public void onError(String message) {
                    isLoading.setValue(false);
                    errorMessage.setValue("Gagal mengunggah foto: " + message);
                }
            });
        } else {
            // Jika tidak ada gambar, langsung simpan
            saveToDatabase(newReview);
        }
    }

    private void saveToDatabase(Review review) {
        repository.saveReviewData(review, new ReviewRepository.ReviewCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                isSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal menyimpan ulasan: " + message);
            }
        });
    }
}