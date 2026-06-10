package com.example.florist.repository;

import com.example.florist.model.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface NotificationCallback {
        void onSuccess(List<Notification> notifications);
        void onError(String message);
    }

    public ListenerRegistration listenToUserNotifications(String userId, NotificationCallback callback) {
        return firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError("Gagal memuat notifikasi: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<Notification> notificationList = new ArrayList<>();
                        for (DocumentSnapshot doc : value) {
                            Notification notif = doc.toObject(Notification.class);
                            if (notif != null) {
                                if (notif.getNotificationId() == null) notif.setNotificationId(doc.getId());
                                notificationList.add(notif);
                            }
                        }
                        callback.onSuccess(notificationList);
                    }
                });
    }

    public void markAsRead(String notificationId) {
        firestore.collection("notifications").document(notificationId)
                .update("isRead", true);
    }
}