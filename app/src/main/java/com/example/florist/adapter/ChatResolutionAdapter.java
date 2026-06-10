package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemMessageChatBinding;
import com.example.florist.model.ComplaintMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatResolutionAdapter extends RecyclerView.Adapter<ChatResolutionAdapter.ViewHolder> {

    private final List<ComplaintMessage> messageList = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", new Locale("id", "ID"));

    private String sellerName = "Penjual";
    private String buyerName = "Pelanggan";

    public void setNames(String sellerName, String buyerName) {
        this.sellerName = sellerName;
        this.buyerName = buyerName;
        notifyDataSetChanged();
    }

    public void setMessages(List<ComplaintMessage> newMessages) {
        messageList.clear();
        messageList.addAll(newMessages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMessageChatBinding binding = ItemMessageChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComplaintMessage message = messageList.get(position);
        Context context = holder.itemView.getContext();

        if ("seller".equalsIgnoreCase(message.getSenderRole()) || "Penjual".equalsIgnoreCase(message.getSenderRole())) {
            holder.binding.tvChatName.setText(sellerName);
        } else {
            holder.binding.tvChatName.setText(buyerName);
        }
        holder.binding.tvChatMessage.setText(message.getMessageText());
        if (message.getCreatedAt() != null) {
            holder.binding.tvChatTime.setText(sdf.format(message.getCreatedAt().toDate()));
        } else {
            holder.binding.tvChatTime.setText("-");
        }

        if (message.getSenderPhotoUrl() != null && !message.getSenderPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(message.getSenderPhotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .into(holder.binding.imgAvatar);
        } else {
            holder.binding.imgAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemMessageChatBinding binding;

        public ViewHolder(@NonNull ItemMessageChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}