package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Complaint;
import com.example.florist.model.ComplaintMessage;
import com.example.florist.model.MaintenanceLog;
import com.example.florist.model.Order;
import com.example.florist.model.Rental;
import com.example.florist.model.TimelineEvent;
import com.example.florist.repository.RentalRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RentalDetailViewModel extends ViewModel {

    private final RentalRepository repository = new RentalRepository();

    private ListenerRegistration complaintListener;
    private ListenerRegistration chatListener;
    private ListenerRegistration maintenanceListener;
    private ListenerRegistration extensionListener;

    private final MutableLiveData<Complaint> activeComplaint = new MutableLiveData<>();
    private final MutableLiveData<String> originalPlantImageUrl = new MutableLiveData<>();
    private final MutableLiveData<List<MaintenanceLog>> maintenanceLogs = new MutableLiveData<>();
    private final MutableLiveData<List<Complaint>> complaintList = new MutableLiveData<>();
    private final MutableLiveData<Rental> activeRental = new MutableLiveData<>();
    private final MutableLiveData<List<ComplaintMessage>> chatMessages = new MutableLiveData<>();
    private final MutableLiveData<Order> activeOrder = new MutableLiveData<>();
    private final MutableLiveData<List<TimelineEvent>> unifiedTimeline = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isResolveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final MutableLiveData<Boolean> showExtensionBanner = new MutableLiveData<>(false);
    private final MutableLiveData<String> extensionDaysText = new MutableLiveData<>("");
    private final MutableLiveData<String> midtransRedirectUrl = new MutableLiveData<>();

    public LiveData<Complaint> getActiveComplaint() { return activeComplaint; }
    public LiveData<Rental> getActiveRental() {return activeRental;}
    public LiveData<String> getOriginalPlantImageUrl() {return originalPlantImageUrl;}
    public LiveData<List<MaintenanceLog>> getMaintenanceLogs() {return maintenanceLogs;}
    public LiveData<List<TimelineEvent>> getUnifiedTimeline() { return unifiedTimeline; }
    public LiveData<List<Complaint>> getComplaintList() {return complaintList;}
    public LiveData<List<ComplaintMessage>> getChatMessages() { return chatMessages; }
    public LiveData<Order> getActiveOrder() { return activeOrder; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsResolveSuccess() { return isResolveSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getShowExtensionBanner() { return showExtensionBanner; }
    public LiveData<String> getExtensionDaysText() { return extensionDaysText; }
    public LiveData<String> getMidtransRedirectUrl() { return midtransRedirectUrl; }

    public void resolveComplaint(String rentalId, String complaintId, String responseText) {
        isLoading.setValue(true);
        repository.resolveComplaint(rentalId, complaintId, responseText, new RentalRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                isResolveSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal mengirim resolusi: " + message);
            }
        });
    }

    public void fetchOrderDetail(String orderId) {
        repository.fetchOrderDetail(orderId, new RentalRepository.DataCallback<Order>() {
            @Override
            public void onSuccess(Order data) { activeOrder.setValue(data); }
            @Override
            public void onError(String message) { errorMessage.setValue(message); }
        });
    }

    public void fetchRentalAndTimeline(String rentalId) {
        repository.fetchRental(rentalId, new RentalRepository.DataCallback<Rental>() {
            @Override
            public void onSuccess(Rental rental) {
                activeRental.setValue(rental);

                // LOGIKA BISNIS DI VIEWMODEL
                if ("SEWA AKTIF".equalsIgnoreCase(rental.getStatus()) && rental.getEndDate() != null) {
                    long endMillis = rental.getEndDate().toDate().getTime();
                    long diffMillis = endMillis - System.currentTimeMillis();
                    long daysLeft = diffMillis / (1000 * 60 * 60 * 24);

                    if (daysLeft >= 0 && daysLeft <= 3) {
                        showExtensionBanner.setValue(true);
                        extensionDaysText.setValue("⏳ Sewa berakhir dalam " + daysLeft + " Hari");
                    } else {
                        showExtensionBanner.setValue(false);
                    }
                } else {
                    showExtensionBanner.setValue(false);
                }

                if (maintenanceListener != null) maintenanceListener.remove();
                maintenanceListener = repository.listenToMaintenanceLogs(rentalId, new RentalRepository.DataCallback<List<MaintenanceLog>>() {
                    @Override
                    public void onSuccess(List<MaintenanceLog> data) { maintenanceLogs.setValue(data); }
                    @Override
                    public void onError(String message) {}
                });
            }

            @Override
            public void onError(String message) { errorMessage.setValue(message); }
        });
    }

    public void fetchComplaintDetail(String rentalId) {
        isLoading.setValue(true);
        if (complaintListener != null) complaintListener.remove();

        complaintListener = repository.listenToComplaintDetail(rentalId, new RentalRepository.DataCallback<List<Complaint>>() {
            @Override
            public void onSuccess(List<Complaint> list) {
                isLoading.setValue(false);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    list.sort((c1, c2) -> {
                        if (c1.getCreatedAt() == null || c2.getCreatedAt() == null) return 0;
                        return c1.getCreatedAt().compareTo(c2.getCreatedAt());
                    });
                }
                complaintList.setValue(list);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void updateComplaintStatus(String rentalId, String complaintId, String newStatus, String resolutionType) {
        repository.updateComplaintStatus(rentalId, complaintId, newStatus, resolutionType, new RentalRepository.ActionCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onError(String message) { errorMessage.setValue(message); }
        });
    }

    public void listenToDiscussion(String rentalId) {
        if (chatListener != null) chatListener.remove();
        chatListener = repository.listenToDiscussion(rentalId, new RentalRepository.DataCallback<List<ComplaintMessage>>() {
            @Override
            public void onSuccess(List<ComplaintMessage> data) { chatMessages.setValue(data); }
            @Override
            public void onError(String message) {}
        });
    }

    public void sendChatMessage(String rentalId, String complaintId, String role, String name, String photoUrl, String text) {
        ComplaintMessage newMessage = new ComplaintMessage(null, role, name, photoUrl, text, Timestamp.now());
        repository.sendChatMessage(rentalId, newMessage);
    }

    public void fetchUnifiedTimeline(String rentalId) {
        repository.fetchUnifiedTimeline(rentalId, new RentalRepository.DataCallback<List<TimelineEvent>>() {
            @Override
            public void onSuccess(List<TimelineEvent> allEvents) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    allEvents.sort((e1, e2) -> {
                        if (e1.getTimestamp() == null || e2.getTimestamp() == null) return 0;
                        return e1.getTimestamp().compareTo(e2.getTimestamp());
                    });
                }
                unifiedTimeline.setValue(allEvents);
            }

            @Override
            public void onError(String message) { errorMessage.setValue(message); }
        });
    }

    public void requestExtensionPayment(Rental rental, double extensionPrice, int extensionDays) {
        isLoading.setValue(true);
        String extId = "EXT-" + System.currentTimeMillis();

        Map<String, Object> extData = new HashMap<>();
        extData.put("extId", extId);
        extData.put("rentalId", rental.getRentalId());
        extData.put("buyerId", rental.getBuyerId());
        extData.put("buyerName", rental.getBuyerName());
        extData.put("plantName", rental.getPlantName());
        extData.put("amount", extensionPrice);
        extData.put("extensionDays", extensionDays);
        extData.put("status", "PENDING");
        extData.put("createdAt", Timestamp.now());

        if (extensionListener != null) extensionListener.remove();

        extensionListener = repository.requestExtensionPayment(extId, extData, new RentalRepository.ExtensionCallback() {
            @Override
            public void onRedirectUrlReceived(String url) {
                isLoading.setValue(false);
                midtransRedirectUrl.setValue(url);
                if (extensionListener != null) extensionListener.remove();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
                if (extensionListener != null) extensionListener.remove();
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (complaintListener != null) complaintListener.remove();
        if (chatListener != null) chatListener.remove();
        if (maintenanceListener != null) maintenanceListener.remove();
        if (extensionListener != null) extensionListener.remove();
    }
}