package com.example.florist.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemChatRoomBinding;
import com.example.florist.model.ChatRoom;
import com.example.florist.views.chat.ChatRoomActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder> {

    private final Context context;
    private final List<ChatRoom> roomList = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));

    private final HashMap<String, String> nameCache = new HashMap<>();
    private final HashMap<String, String> imageCache = new HashMap<>();

    public ChatRoomAdapter(Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
    }

    public void setRoomList(List<ChatRoom> newRooms) {
        this.roomList.clear();
        this.roomList.addAll(newRooms);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatRoomBinding binding = ItemChatRoomBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatRoom room = roomList.get(position);

        String targetId, fallbackName, fallbackImage;
        boolean isCurrentUserBuyer = currentUserId.equals(room.getBuyerId());

        if (isCurrentUserBuyer) {
            targetId = room.getSellerId();
            fallbackName = room.getSellerName();
            fallbackImage = room.getSellerImageUrl();
        } else {
            targetId = room.getBuyerId();
            fallbackName = room.getBuyerName();
            fallbackImage = "";
        }

        int unreadCount = isCurrentUserBuyer ? room.getUnreadBuyer() : room.getUnreadSeller();

        holder.binding.tvLastMessage.setText(room.getLastMessage().isEmpty() ? "Belum ada pesan" : room.getLastMessage());

        if (unreadCount > 0) {
            holder.binding.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.binding.tvUnreadBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));

            holder.binding.tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.black));
        } else {
            holder.binding.tvUnreadBadge.setVisibility(View.GONE);

            holder.binding.tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.gray_500));
        }

        if (room.getLastMessageTime() != null) {
            holder.binding.tvLastMessageTime.setText(timeFormat.format(room.getLastMessageTime().toDate()));
        }

        if (nameCache.containsKey(targetId)) {
            holder.binding.tvRoomUserName.setText(nameCache.get(targetId));
            Glide.with(context).load(imageCache.get(targetId))
                    .placeholder(R.drawable.user).error(R.drawable.user)
                    .circleCrop().into(holder.binding.imgUserAvatar);
        } else {
            holder.binding.tvRoomUserName.setText("Memuat...");
            Glide.with(context).load(R.drawable.user).circleCrop().into(holder.binding.imgUserAvatar);

            String collection = isCurrentUserBuyer ? "shops" : "users";
            FirebaseFirestore.getInstance().collection(collection).document(targetId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String realName = isCurrentUserBuyer ? doc.getString("shopName") : doc.getString("username");
                            String realImage = isCurrentUserBuyer ? doc.getString("shopImageUrl") : doc.getString("profileImageUrl");

                            if (realName == null) realName = fallbackName != null ? fallbackName : "Pengguna Setaman";
                            if (realImage == null) realImage = fallbackImage;

                            nameCache.put(targetId, realName);
                            imageCache.put(targetId, realImage);

                            holder.binding.tvRoomUserName.setText(realName);
                            Glide.with(context).load(realImage)
                                    .placeholder(R.drawable.user).error(R.drawable.user)
                                    .circleCrop().into(holder.binding.imgUserAvatar);
                        }
                    });
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatRoomActivity.class);
            intent.putExtra("EXTRA_TARGET_ID", targetId);

            intent.putExtra("EXTRA_TARGET_NAME", nameCache.containsKey(targetId) ? nameCache.get(targetId) : fallbackName);
            intent.putExtra("EXTRA_TARGET_IMAGE", imageCache.containsKey(targetId) ? imageCache.get(targetId) : fallbackImage);

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemChatRoomBinding binding;
        public ViewHolder(ItemChatRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}