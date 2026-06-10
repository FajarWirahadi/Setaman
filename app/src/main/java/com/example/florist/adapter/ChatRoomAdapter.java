package com.example.florist.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemChatRoomBinding;
import com.example.florist.model.ChatRoom;
import com.example.florist.views.chat.ChatRoomActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder> {

    private final Context context;
    private final List<ChatRoom> roomList = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));

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

        String targetId, targetName, targetImageUrl;

        if (currentUserId.equals(room.getBuyerId())) {
            targetId = room.getSellerId();
            targetName = room.getSellerName();
            targetImageUrl = room.getSellerImageUrl();
        } else {
            targetId = room.getBuyerId();
            targetName = room.getBuyerName();
            targetImageUrl = "";
        }

        holder.binding.tvRoomUserName.setText(targetName != null ? targetName : "Pengguna Setaman");
        holder.binding.tvLastMessage.setText(room.getLastMessage().isEmpty() ? "Belum ada pesan" : room.getLastMessage());

        if (room.getLastMessageTime() != null) {
            holder.binding.tvLastMessageTime.setText(timeFormat.format(room.getLastMessageTime().toDate()));
        }

        Glide.with(context)
                .load(targetImageUrl)
                .placeholder(R.drawable.building)
                .error(R.drawable.building)
                .circleCrop()
                .into(holder.binding.imgUserAvatar);


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatRoomActivity.class);
            intent.putExtra("EXTRA_TARGET_ID", targetId);
            intent.putExtra("EXTRA_TARGET_NAME", targetName);
            intent.putExtra("EXTRA_TARGET_IMAGE", targetImageUrl);
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