package com.example.florist.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.florist.R;
import com.example.florist.views.buyer.BuyerDetailActivity;
import com.example.florist.views.chat.ChatRoomActivity;
import com.example.florist.views.homepage.HomepageActivity;
import com.example.florist.views.seller.OwnerDashboardActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "Setaman_Delivery_Channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();

        String title = "Notifikasi Setaman";
        String body = "Anda memiliki pesan baru.";

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }
        else if (data.containsKey("title") && data.containsKey("body")) {
            title = data.get("title");
            body = data.get("body");
        }

        showNotification(title, body, data);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    private void showNotification(String title, String body, Map<String, String> data) {
        Intent intent;

        String type = data.get("type");
        if (type == null) type = "default";

        switch (type) {
            case "chat":
                intent = new Intent(this, ChatRoomActivity.class);
                intent.putExtra("EXTRA_TARGET_ID", data.get("targetId"));
                intent.putExtra("EXTRA_TARGET_NAME", data.get("targetName"));
                intent.putExtra("EXTRA_ROOM_ID", data.get("roomId"));
                intent.putExtra("EXTRA_TARGET_IMAGE", "");
                break;

            case "new_order":
                intent = new Intent(this, OwnerDashboardActivity.class);
                intent.putExtra("EXTRA_ORDER_ID", data.get("orderId"));
                break;

            case "product":
                intent = new Intent(this, BuyerDetailActivity.class);
                intent.putExtra("EXTRA_PRODUCT_ID", data.get("productId"));
                break;

            case "delivery_update":
                intent = new Intent(this, HomepageActivity.class);

                intent.putExtra("navigate_to", "transaction_tab");

                intent.putExtra("EXTRA_ORDER_ID", data.get("orderId"));
                break;

            case "rental_update":
                intent = new Intent(this, com.example.florist.views.seller.RentalDetailActivity.class);
                intent.putExtra("RENTAL_ID", data.get("rentalId"));
                break;
            default:
                intent = new Intent(this, HomepageActivity.class);
                break;
        }

        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notifikasi Pesanan Setaman",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_icon)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}