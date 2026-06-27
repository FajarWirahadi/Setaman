package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.databinding.ItemSellerComplaintBinding;
import com.example.florist.model.Complaint;
import com.example.florist.utils.StatusBadgeHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SellerComplaintAdapter extends RecyclerView.Adapter<SellerComplaintAdapter.ComplaintViewHolder> {

    private final Context context;
    private final List<Complaint> complaintList = new ArrayList<>();
    private final OnComplaintClickListener listener;

    public interface OnComplaintClickListener {
        void onClick(Complaint complaint);
    }

    public SellerComplaintAdapter(Context context, OnComplaintClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<Complaint> newList) {
        complaintList.clear();
        complaintList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSellerComplaintBinding binding = ItemSellerComplaintBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ComplaintViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ComplaintViewHolder holder, int position) {
        Complaint complaint = complaintList.get(position);

        String buyerName = complaint.getBuyerName() != null ? complaint.getBuyerName() : "Pembeli";
        holder.binding.tvBuyerName.setText(buyerName);

        if (complaint.getBuyerImageUrl() != null && !complaint.getBuyerImageUrl().isEmpty()) {
            holder.binding.imgBuyerProfile.setVisibility(android.view.View.VISIBLE);
            holder.binding.tvBuyerInitial.setVisibility(android.view.View.GONE);
            Glide.with(context).load(complaint.getBuyerImageUrl()).into(holder.binding.imgBuyerProfile);
        } else {
            holder.binding.imgBuyerProfile.setVisibility(android.view.View.GONE);
            holder.binding.tvBuyerInitial.setVisibility(android.view.View.VISIBLE);
            if (!buyerName.isEmpty()) {
                holder.binding.tvBuyerInitial.setText(String.valueOf(buyerName.charAt(0)).toUpperCase());
            } else {
                holder.binding.tvBuyerInitial.setText("P");
            }
        }

        holder.binding.tvPlantName.setText(complaint.getPlantName() != null ? complaint.getPlantName() : "Tanaman");
        holder.binding.tvReason.setText("Kendala: " + complaint.getReason());

        String rawStatus = complaint.getStatus() != null ? complaint.getStatus() : "MENUNGGU RESPON";
       StatusBadgeHelper.applyStatus(context, holder.binding.tvComplaintStatus, rawStatus);

        holder.binding.tvOrderId.setText(complaint.getOrderId() != null ? "Sewa #" + complaint.getOrderId() : "ID Sewa Tidak Tersedia");
        holder.binding.tvRentalDuration.setText(complaint.getRentalDuration() != null ? "Masa Sewa: " + complaint.getRentalDuration() : "Masa Sewa: -");

        if (complaint.getCreatedAt() != null) {
            String dateStr = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"))
                    .format(complaint.getCreatedAt().toDate());
            holder.binding.tvDate.setText("Diajukan pada: " + dateStr);
        }

        if (complaint.getEvidenceImageUrl() != null && !complaint.getEvidenceImageUrl().isEmpty()) {
            Glide.with(context).load(complaint.getEvidenceImageUrl()).into(holder.binding.imgPlant);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(complaint));
        holder.binding.btnRespond.setOnClickListener(v -> listener.onClick(complaint));
    }

    @Override
    public int getItemCount() {
        return complaintList.size();
    }

    public static class ComplaintViewHolder extends RecyclerView.ViewHolder {
        ItemSellerComplaintBinding binding;
        public ComplaintViewHolder(ItemSellerComplaintBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}