package com.example.florist.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.florist.databinding.ItemNotificationBinding;
import com.example.florist.model.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<Notification> notificationList = new ArrayList<>();
    private final OnNotificationClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, HH:mm", new Locale("id", "ID"));

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<Notification> newNotifications) {
        notificationList.clear();
        notificationList.addAll(newNotifications);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notif = notificationList.get(position);

        holder.binding.tvNotifTitle.setText(notif.getTitle());
        holder.binding.tvNotifBody.setText(notif.getBody());

        if (notif.getCreatedAt() != null) {
            holder.binding.tvNotifDate.setText(dateFormat.format(notif.getCreatedAt().toDate()));
        }

        // Tampilan khusus jika BELUM DIBACA
        if (!notif.isRead()) {
            holder.binding.layoutContainer.setBackgroundColor(Color.parseColor("#F5F9FF"));
            holder.binding.imgUnreadDot.setVisibility(View.VISIBLE);
        } else {
            holder.binding.layoutContainer.setBackgroundColor(Color.WHITE);
            holder.binding.imgUnreadDot.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notif);
            }
        });
    }

    @Override
    public int getItemCount() { return notificationList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemNotificationBinding binding;
        ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}