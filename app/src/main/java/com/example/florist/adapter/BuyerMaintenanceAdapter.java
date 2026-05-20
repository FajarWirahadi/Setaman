package com.example.florist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.databinding.ItemMaintenanceLogBuyerBinding;
import com.example.florist.model.MaintenanceLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuyerMaintenanceAdapter extends RecyclerView.Adapter<BuyerMaintenanceAdapter.ViewHolder> {

    private final List<MaintenanceLog> logs = new ArrayList<>();
    private final OnLogActionListener listener;

    public interface OnLogActionListener {
        void onChatSellerClicked(MaintenanceLog log);
    }

    public BuyerMaintenanceAdapter(OnLogActionListener listener) {
        this.listener = listener;
    }

    public void setLogs(List<MaintenanceLog> newLogs) {
        logs.clear();
        logs.addAll(newLogs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMaintenanceLogBuyerBinding binding = ItemMaintenanceLogBuyerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaintenanceLog log = logs.get(position);

        holder.binding.tvLogDescription.setText(log.getDescription()); //

        if (log.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id", "ID"));
            holder.binding.tvLogDate.setText(sdf.format(log.getCreatedAt().toDate())); //
        }

        Glide.with(holder.itemView.getContext())
                .load(log.getImageUrl()) //
                .into(holder.binding.imgLogPhoto);

        holder.binding.viewLineTop.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.binding.viewLineBottom.setVisibility(position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);

        holder.binding.btnChatSeller.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatSellerClicked(log);
            }
        });
    }

    @Override
    public int getItemCount() { return logs.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemMaintenanceLogBuyerBinding binding;
        ViewHolder(ItemMaintenanceLogBuyerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}