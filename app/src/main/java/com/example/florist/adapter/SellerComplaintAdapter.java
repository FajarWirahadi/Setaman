package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemSellerComplaintBinding;
import com.example.florist.model.Rental;

import java.util.ArrayList;
import java.util.List;

public class SellerComplaintAdapter extends RecyclerView.Adapter<SellerComplaintAdapter.ViewHolder> {

    private final Context context;
    private final List<Rental> complaintRentals = new ArrayList<>();
    private final OnComplaintClickListener listener;

    public interface OnComplaintClickListener {
        void onRespondClicked(Rental rental);
    }

    public SellerComplaintAdapter(Context context, OnComplaintClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<Rental> newList) {
        complaintRentals.clear();
        complaintRentals.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSellerComplaintBinding binding = ItemSellerComplaintBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Rental rental = complaintRentals.get(position);

        String buyerName = rental.getReceiverName() != null ? rental.getReceiverName() : "Pelanggan";
        String plantName = rental.getPlantName() != null ? rental.getPlantName() : "Tanaman";
        String status = rental.getStatus();

        if (!buyerName.isEmpty()) {
            holder.binding.tvBuyerInitial.setText(buyerName.substring(0, 1).toUpperCase());
        }

        holder.binding.tvBuyerName.setText(buyerName + " mengajukan komplain");

        holder.binding.tvPlantName.setText(plantName);
        holder.binding.tvOrderId.setText("Sewa #" + rental.getRentalId());

        Glide.with(context)
                .load(rental.getPlantImageUrl())
                .placeholder(android.R.color.darker_gray)
                .into(holder.binding.imgPlant);

        holder.binding.imgPlantContainer.setOnClickListener(v -> {
            // TODO: Nanti ganti dengan Intent ke halaman Full Screen Image
            Toast.makeText(context, "Membuka foto: " + rental.getPlantName(), Toast.LENGTH_SHORT).show();
        });

        holder.binding.btnRespond.bringToFront();
        holder.binding.imgPlantContainer.bringToFront();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", new java.util.Locale("id", "ID"));
        String startDate = rental.getStartDate() != null ? sdf.format(rental.getStartDate().toDate()) : "-";
        String endDate = rental.getEndDate() != null ? sdf.format(rental.getEndDate().toDate()) : "-";
        holder.binding.tvRentalDuration.setText("Masa Sewa: " + startDate + " s/d " + endDate);

        if ("Pending".equalsIgnoreCase(status) || "Komplain".equalsIgnoreCase(status)) {
            holder.binding.tvComplaintStatus.setText("Butuh Tindakan");
            holder.binding.tvComplaintStatus.setTextColor(ContextCompat.getColor(context, R.color.red_500));
            holder.binding.cardLeftIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.red_500));
            holder.binding.iconIndicator.setStrokeColor(ContextCompat.getColor(context, R.color.red_200));
            holder.binding.iconIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.red_50));
            holder.binding.tvBuyerInitial.setTextColor(ContextCompat.getColor(context, R.color.red_600));
            holder.binding.btnRespond.setVisibility(View.VISIBLE);

        } else if ("PROSES PERBAIKAN".equalsIgnoreCase(status)) {
            holder.binding.tvComplaintStatus.setText("Sedang Diperbaiki");
            holder.binding.tvComplaintStatus.setTextColor(ContextCompat.getColor(context, R.color.blue_600));
            holder.binding.cardLeftIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.blue_500));
            holder.binding.iconIndicator.setStrokeColor(ContextCompat.getColor(context, R.color.blue_300));
            holder.binding.iconIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.blue_50));
            holder.binding.tvBuyerInitial.setTextColor(ContextCompat.getColor(context, R.color.blue_700));
            holder.binding.btnRespond.setVisibility(View.GONE);

        } else if ("Menunggu Konfirmasi".equalsIgnoreCase(status)) {
            holder.binding.tvComplaintStatus.setText("Menunggu Pembeli");
            holder.binding.tvComplaintStatus.setTextColor(ContextCompat.getColor(context, R.color.yellow_600));
            holder.binding.cardLeftIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow_500));
            holder.binding.iconIndicator.setStrokeColor(ContextCompat.getColor(context, R.color.yellow_300));
            holder.binding.iconIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.yellow_50));
            holder.binding.tvBuyerInitial.setTextColor(ContextCompat.getColor(context, R.color.yellow_700));
            holder.binding.btnRespond.setVisibility(View.GONE);

        } else {
            holder.binding.tvComplaintStatus.setText("Selesai");
            holder.binding.tvComplaintStatus.setTextColor(ContextCompat.getColor(context, R.color.green_600));
            holder.binding.cardLeftIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.green_500));
            holder.binding.iconIndicator.setStrokeColor(ContextCompat.getColor(context, R.color.green_300));
            holder.binding.iconIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.green_50));
            holder.binding.tvBuyerInitial.setTextColor(ContextCompat.getColor(context, R.color.green_700));
            holder.binding.btnRespond.setVisibility(View.GONE);
        }

        holder.binding.btnRespond.setOnClickListener(v -> listener.onRespondClicked(rental));
        holder.binding.cardContent.setOnClickListener(v -> listener.onRespondClicked(rental));
    }

    @Override
    public int getItemCount() { return complaintRentals.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ItemSellerComplaintBinding binding;
        public ViewHolder(ItemSellerComplaintBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}