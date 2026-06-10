package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Notification;
import com.example.florist.repository.NotificationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class NotificationViewModel extends ViewModel {
    private final NotificationRepository repository = new NotificationRepository();
    private ListenerRegistration registration;

    private final MutableLiveData<List<Notification>> notifications = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<List<Notification>> getNotifications() { return notifications; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadMyNotifications() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            registration = repository.listenToUserNotifications(currentUser.getUid(), new NotificationRepository.NotificationCallback() {
                @Override
                public void onSuccess(List<Notification> data) {
                    notifications.setValue(data);
                }

                @Override
                public void onError(String message) {
                    errorMessage.setValue(message);
                }
            });
        }
    }

    public void markNotificationAsRead(String notificationId) {
        repository.markAsRead(notificationId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (registration != null) {
            registration.remove();
        }
    }
}