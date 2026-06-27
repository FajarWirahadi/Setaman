package com.example.florist.viewmodels;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.florist.model.Complaint;
import com.example.florist.model.ComplaintMessage;
import com.example.florist.repository.ComplaintRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ComplaintViewModel extends ViewModel {

    private final ComplaintRepository repository = new ComplaintRepository();
    private ListenerRegistration complaintListener, chatListener;

    private final MutableLiveData<List<Complaint>> complaintList = new MutableLiveData<>();
    private final MutableLiveData<List<ComplaintMessage>> chatMessages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();

    public LiveData<List<Complaint>> getComplaintList() {return complaintList;}
    public LiveData<List<ComplaintMessage>> getChatMessages() {return chatMessages;}
    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<Boolean> getIsSuccess() {return isSuccess;}
    public LiveData<String> getErrorMessage() {return errorMessage;}

    public void submitComplaint(String rentalId, String orderId, String rentalDuration,
                                String buyerId, String buyerImageUrl, String sellerId,
                                String plantName, String buyerName,
                                String reason, String desc, Uri imageUri) {

        if (rentalId == null || rentalId.isEmpty()) {
            errorMessage.setValue("ID Pesanan tidak ditemukan. Harap kembali dan coba lagi.");
            return;
        }

        isLoading.setValue(true);

        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {}

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                String complaintId = UUID.randomUUID().toString();

                Complaint complaint = new Complaint();
                complaint.setComplaintId(complaintId);
                complaint.setRentalId(rentalId);
                complaint.setOrderId(orderId);
                complaint.setRentalDuration(rentalDuration);
                complaint.setBuyerId(buyerId);
                complaint.setBuyerImageUrl(buyerImageUrl);
                complaint.setSellerId(sellerId);
                complaint.setPlantName(plantName);
                complaint.setBuyerName(buyerName);
                complaint.setReason(reason);
                complaint.setDescription(desc);
                complaint.setEvidenceImageUrl(imageUrl);
                complaint.setStatus("MENUNGGU RESPON");
                complaint.setCreatedAt(Timestamp.now());

                // SERAHKAN KE REPOSITORY
                repository.submitComplaintWithRentalUpdate(complaint, new ComplaintRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        isLoading.postValue(false);
                        isSuccess.postValue(true);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue(message);
                    }
                });
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                isLoading.postValue(false);
                errorMessage.postValue("Gagal mengunggah foto: " + error.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                isLoading.postValue(false);
                errorMessage.postValue("Upload ditunda karena masalah jaringan.");
            }
        }).dispatch();
    }

    public void fetchComplaints(String rentalId) {
        if (complaintListener != null) complaintListener.remove();

        complaintListener = repository.listenToComplaints(rentalId, new ComplaintRepository.ComplaintListCallback() {
            @Override
            public void onDataFetched(List<Complaint> complaints) {
                complaintList.setValue(complaints);
            }

            @Override
            public void onError(String message) {
                errorMessage.setValue(message);
            }
        });
    }

    public void acceptResolution(String complaintId) {
        isLoading.setValue(true);
        repository.acceptResolution(complaintId, new ComplaintRepository.ActionCallback() {
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

    public void rejectResolution(String complaintId, String rejectionReason) {
        isLoading.setValue(true);
        repository.rejectResolution(complaintId, rejectionReason, new ComplaintRepository.ActionCallback() {
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

    public void listenToDiscussion(String rentalId) {
        if (chatListener != null) chatListener.remove();

        chatListener = repository.listenToDiscussion(rentalId, new ComplaintRepository.ChatListCallback() {
            @Override
            public void onDataFetched(List<ComplaintMessage> messages) {
                chatMessages.setValue(messages);
            }

            @Override
            public void onError(String message) {
                errorMessage.setValue(message);
            }
        });
    }

    public void sendChatMessage(String rentalId, String role, String name, String photoUrl, String text) {
        if (rentalId == null) {
            errorMessage.setValue("Gagal mengirim: Data Pesanan tidak valid.");
            return;
        }

        ComplaintMessage newMessage = new ComplaintMessage(null, role, name, photoUrl, text, Timestamp.now());

        repository.sendChatMessage(rentalId, newMessage, new ComplaintRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                // Berhasil kirim, tidak perlu aksi khusus karena listener akan mengupdate UI otomatis
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
        if (complaintListener != null) complaintListener.remove();
        if (chatListener != null) chatListener.remove();
    }
}