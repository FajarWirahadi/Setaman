package com.example.florist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.databinding.ItemChatMessageBinding;
import com.example.florist.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<ChatMessage> messageList = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));

    public interface OnQuoteClickListener {
        void onQuoteClicked(String referenceId, String referenceType, String rentalId);
    }
    private OnQuoteClickListener quoteClickListener;

    public void setQuoteClickListener(OnQuoteClickListener listener) {
        this.quoteClickListener = listener;
    }

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void updateMessages(List<ChatMessage> newMessages) {
        this.messageList.clear();
        this.messageList.addAll(newMessages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatMessageBinding binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        String timeString = "";
        if (message.getTimestamp() != null) {
            timeString = timeFormat.format(message.getTimestamp().toDate());
        }

        boolean hasImage = message.getImageUrl() != null && !message.getImageUrl().isEmpty();
        boolean hasReference = message.getReferenceId() != null && !message.getReferenceId().isEmpty();
        String refDesc = message.getReferenceDesc();

        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            holder.binding.layoutSend.setVisibility(View.VISIBLE);
            holder.binding.layoutReceive.setVisibility(View.GONE);
            holder.binding.tvMessageSend.setText(message.getText());
            holder.binding.tvTimeSend.setText(timeString);

            if (hasImage) {
                holder.binding.cardImageSend.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(message.getImageUrl())
                        .into(holder.binding.imgMessageSend);
            } else {
                holder.binding.cardImageSend.setVisibility(View.GONE);
            }

            if (hasReference) {
                holder.binding.layoutRefSend.setVisibility(View.VISIBLE);

                if ("COMPLAINT".equals(message.getReferenceType())) {
                    holder.binding.tvRefTextSend.setText("Terkait Komplain Pesanan " + refDesc);
                } else if ("MAINTENANCE".equals(message.getReferenceType())) {
                    holder.binding.tvRefTextSend.setText("Terkait Jadwal Perawatan " + refDesc);
                } else {
                    holder.binding.tvRefTextSend.setText("Terkait Pesanan " + refDesc);
                }
                holder.binding.layoutRefSend.setOnClickListener(v -> {
                    if (quoteClickListener != null) {
                        quoteClickListener.onQuoteClicked(message.getReferenceId(), message.getReferenceType(), message.getRentalId());
                    }
                });
            } else {
                holder.binding.layoutRefSend.setVisibility(View.GONE);
            }

        } else {
            // ==========================================
            // SISI PENERIMA (Kiri)
            // ==========================================
            holder.binding.layoutReceive.setVisibility(View.VISIBLE);
            holder.binding.layoutSend.setVisibility(View.GONE);
            holder.binding.tvMessageReceive.setText(message.getText());
            holder.binding.tvTimeReceive.setText(timeString);

            if (hasImage) {
                holder.binding.cardImageReceive.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(message.getImageUrl())
                        .into(holder.binding.imgMessageReceive);
            } else {
                holder.binding.cardImageReceive.setVisibility(View.GONE);
            }

            if (hasReference) {
                holder.binding.layoutRefReceive.setVisibility(View.VISIBLE);

                if ("COMPLAINT".equals(message.getReferenceType())) {
                    holder.binding.tvRefTextReceive.setText("Terkait Komplain Pesanan" + refDesc);
                } else if ("MAINTENANCE".equals(message.getReferenceType())) {
                    holder.binding.tvRefTextReceive.setText("Terkait Jadwal Perawatan " + refDesc);
                } else {
                    holder.binding.tvRefTextReceive.setText("Terkait Pesanan " + refDesc);
                }
                holder.binding.layoutRefReceive.setOnClickListener(v -> {
                    if (quoteClickListener != null) {
                        quoteClickListener.onQuoteClicked(message.getReferenceId(), message.getReferenceType(), message.getRentalId());
                    }
                });
            } else {
                holder.binding.layoutRefReceive.setVisibility(View.GONE);
            }

        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        ItemChatMessageBinding binding;

        public MessageViewHolder(ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}