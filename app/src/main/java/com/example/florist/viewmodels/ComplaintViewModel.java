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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ComplaintViewModel extends ViewModel {

    private ListenerRegistration complaintListener, chatListener;
    private final ComplaintRepository repository = new ComplaintRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
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

    public void submitComplaint(String rentalId, String reason, String desc, Uri imageUri) {
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
                try {
                    String imageUrl = (String) resultData.get("secure_url");
                    String complaintId = UUID.randomUUID().toString();
                    Complaint complaint = new Complaint(
                            complaintId, reason, desc, imageUrl, "Pending", Timestamp.now()
                    );

                    repository.submitComplaint(rentalId, complaint, new ComplaintRepository.OnActionCallback() {
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
                } catch (Exception e) {
                    isLoading.postValue(false);
                    errorMessage.postValue("Kesalahan sistem: " + e.getMessage());
                }
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
        complaintListener = db.collection("rentals").document(rentalId).collection("complaints")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        java.util.List<Complaint> list = new java.util.ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            Complaint c = doc.toObject(Complaint.class);
                            if (c != null) list.add(c);
                        }
                        complaintList.setValue(list);
                    }
                });
    }

    public void acceptResolution(String rentalId, String complaintId) {
        isLoading.setValue(true);
        WriteBatch batch = db.batch();

        DocumentReference compRef = db.collection("rentals").document(rentalId)
                .collection("complaints").document(complaintId);
        batch.update(compRef, "status", "Resolved", "resolvedAt", Timestamp.now());

        DocumentReference rentalRef = db.collection("rentals").document(rentalId);
        batch.update(rentalRef, "status", "AKTIF", "hasComplaint", false);

        batch.commit().addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                isSuccess.setValue(true);
            } else {
                errorMessage.setValue("Gagal menyelesaikan: " + task.getException().getMessage());
            }
        });
    }

    public void rejectResolution(String rentalId, String complaintId) {
        isLoading.setValue(true);
        WriteBatch batch = db.batch();

        DocumentReference rentalRef = db.collection("rentals").document(rentalId);
        DocumentReference complaintRef = rentalRef.collection("complaints").document(complaintId);

        batch.update(rentalRef, "status", "Komplain");
        batch.update(complaintRef, "status", "Komplain");

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    isSuccess.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Gagal menolak resolusi: " + e.getMessage());
                });
    }

    public void listenToDiscussion(String rentalId) {
        if (chatListener != null) chatListener.remove();

        chatListener = db.collection("rentals").document(rentalId)
                .collection("discussion_messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<ComplaintMessage> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                          ComplaintMessage msg = doc.toObject(ComplaintMessage.class);
                            if (msg != null) list.add(msg);
                        }
                        chatMessages.setValue(list);
                    }
                });
    }

    public void sendChatMessage(String rentalId, String role, String name, String photoUrl, String text) {
        if (rentalId == null) {
            errorMessage.postValue("Gagal mengirim: Data Pesanan tidak valid.");
            return;
        }

        CollectionReference chatRef = db.collection("rentals").document(rentalId)
                .collection("discussion_messages");

        String msgId = chatRef.document().getId();
        ComplaintMessage newMessage = new ComplaintMessage(
                msgId, role, name, photoUrl, text, Timestamp.now()
        );

        chatRef.document(msgId).set(newMessage)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Gagal mengirim pesan: " + e.getMessage());
                });
    }
}