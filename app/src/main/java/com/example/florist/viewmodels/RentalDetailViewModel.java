package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.florist.model.Complaint;
import com.example.florist.model.ComplaintMessage;
import com.example.florist.model.MaintenanceLog;
import com.example.florist.model.Order;
import com.example.florist.model.Rental;
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

public class RentalDetailViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration chatListener;
    private final MutableLiveData<Complaint> activeComplaint = new MutableLiveData<>();
    private final MutableLiveData<String> originalPlantImageUrl = new MutableLiveData<>();
    private final MutableLiveData<List<MaintenanceLog>> maintenanceLogs = new MutableLiveData<>();
    private final MutableLiveData<List<Complaint>> complaintList = new MutableLiveData<>();
    private final MutableLiveData<Rental> activeRental = new MutableLiveData<>();

    private final MutableLiveData<List<ComplaintMessage>> chatMessages = new MutableLiveData<>();
    private final MutableLiveData<Order> activeOrder = new MutableLiveData<>();


    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isResolveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration;

    public LiveData<Complaint> getActiveComplaint() { return activeComplaint; }
    public LiveData<Rental> getActiveRental() {return activeRental;}
    public LiveData<String> getOriginalPlantImageUrl() {return originalPlantImageUrl;}
    public LiveData<List<MaintenanceLog>> getMaintenanceLogs() {return maintenanceLogs;}
    public LiveData<List<Complaint>> getComplaintList() {return complaintList;}
    public LiveData<List<ComplaintMessage>> getChatMessages() {
        return chatMessages;
    }
    public LiveData<Order> getActiveOrder() { return activeOrder; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsResolveSuccess() { return isResolveSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void resolveComplaint(String rentalId, String complaintId, String responseText) {
        isLoading.setValue(true);
        WriteBatch batch = db.batch();

        DocumentReference complaintRef = db.collection("rentals")
                .document(rentalId)
                .collection("complaints")
                .document(complaintId);

        batch.update(complaintRef,
                "sellerResponseText", responseText,
                "status", "Responded",
                "respondedAt", Timestamp.now());

        DocumentReference rentalRef = db.collection("rentals").document(rentalId);
        batch.update(rentalRef, "status", "Menunggu Konfirmasi");

        batch.commit().addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                isResolveSuccess.setValue(true);
            } else {
                errorMessage.setValue("Gagal mengirim resolusi: " + task.getException().getMessage());
            }
        });
    }

    public void fetchOrderDetail(String orderId) {
        db.collection("orders").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Order order = documentSnapshot.toObject(Order.class);
                activeOrder.setValue(order);
            }
        }).addOnFailureListener(e -> errorMessage.setValue("Gagal memuat info order: " + e.getMessage()));
    }

    public void fetchRentalAndTimeline(String rentalId) {
        db.collection("rentals").document(rentalId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Rental rental = doc.toObject(Rental.class);
                        if (rental != null) {
                            rental.setRentalId(doc.getId());
                            activeRental.setValue(rental);
                        }

                        db.collection("rentals").document(rentalId).collection("maintenance_logs")
                                .orderBy("createdAt", Query.Direction.ASCENDING)
                                .addSnapshotListener((value, error) -> {
                                    if (value != null) {
                                        List<MaintenanceLog> logs = new ArrayList<>();
                                        for (DocumentSnapshot logDoc : value) {
                                            MaintenanceLog log = logDoc.toObject(MaintenanceLog.class);
                                            if (log != null) logs.add(log);
                                        }
                                        maintenanceLogs.setValue(logs);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> errorMessage.setValue("Gagal memuat tanaman: " + e.getMessage()));
    }

    public void fetchComplaintDetail(String rentalId) {
        isLoading.setValue(true);
        if (listenerRegistration != null) listenerRegistration.remove();

        listenerRegistration = db.collection("rentals").document(rentalId).collection("complaints")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);
                    if (error != null) {
                        errorMessage.setValue("Gagal memuat detail komplain: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        List<Complaint> list = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value) {
                            Complaint c = doc.toObject(Complaint.class);
                            if (c != null) list.add(c);
                        }
                        complaintList.setValue(list);
                    }
                });
    }

    public void updateComplaintStatus(String rentalId, String complaintId, String newStatus) {
        WriteBatch batch = db.batch();

        DocumentReference complaintRef = db.collection("rentals")
                .document(rentalId)
                .collection("complaints")
                .document(complaintId);
        batch.update(complaintRef,
                "status", newStatus,
                "respondedAt", com.google.firebase.Timestamp.now());

        DocumentReference rentalRef = db.collection("rentals").document(rentalId);
        batch.update(rentalRef, "status", newStatus);

        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                errorMessage.setValue("Gagal mengubah status: " + task.getException().getMessage());
            }
        });
    }

    public void listenToDiscussion(String rentalId) {
        if (chatListener != null) chatListener.remove();

        chatListener = db.collection("rentals").document(rentalId)
                .collection("discussion_messages")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
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

    public void sendChatMessage(String rentalId, String complaintId, String role, String name, String photoUrl, String text) {
        CollectionReference chatRef = db.collection("rentals").document(rentalId)
                .collection("discussion_messages");

        String msgId = chatRef.document().getId();
        ComplaintMessage newMessage = new ComplaintMessage(
                msgId, role, name, photoUrl, text, Timestamp.now()
        );

        chatRef.document(msgId).set(newMessage);
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}