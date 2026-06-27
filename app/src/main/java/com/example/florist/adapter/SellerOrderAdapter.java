package com.example.florist.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemSellerOrderBinding;
import com.example.florist.model.CartItem;
import com.example.florist.model.Order;
import com.example.florist.utils.StatusBadgeHelper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SellerOrderAdapter extends RecyclerView.Adapter<SellerOrderAdapter.SellerOrderViewHolder> {

    private final Context context;
    private final List<Order> orderList = new ArrayList<>();
    private final OnOrderActionListener actionListener;

    public interface OnOrderActionListener {
        void onAcceptClicked(Order order);
        void onRejectClicked(Order order);
        void onUpdateDeliveryClicked(Order order);
    }

    public SellerOrderAdapter(Context context, OnOrderActionListener actionListener) {
        this.context = context;
        this.actionListener = actionListener;
    }

    public void updateData(List<Order> newOrders) {
        this.orderList.clear();
        this.orderList.addAll(newOrders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SellerOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSellerOrderBinding binding = ItemSellerOrderBinding.inflate(LayoutInflater.from(context), parent, false);
        return new SellerOrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SellerOrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // 1. SETUP LOGIKA ALAMAT & GOOGLE MAPS
        if (order.getDeliveryAddress() != null) {
            holder.binding.layoutAddress.setVisibility(View.VISIBLE);
            holder.binding.tvAddress.setText(order.getDeliveryAddress().getFullAddress());

            // Aksi Buka Google Maps
            holder.binding.layoutAddress.setOnClickListener(v -> {
                double lat = order.getDeliveryAddress().getLatitude();
                double lng = order.getDeliveryAddress().getLongitude();
                String label = order.getDeliveryAddress().getReceiverName();

                // Format URI untuk membuka navigasi rute langsung
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(mapIntent);
                } else {
                    // Fallback jika Google Maps tidak terinstal
                    Uri browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
                    context.startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
                }
            });
        } else {
            holder.binding.layoutAddress.setVisibility(View.GONE);
        }

        holder.binding.tvOrderId.setText(order.getOrderId());
        String status = order.getStatus() != null ? order.getStatus() : "";
        StatusBadgeHelper.applyStatus(context, holder.binding.tvOrderStatus, status);

        if (order.getDeliveryAddress() != null && order.getDeliveryAddress() != null) {
            holder.binding.tvBuyerName.setText(order.getReceiverName());
        } else if (order.getBuyerId() != null) {
            holder.binding.tvBuyerName.setText("ID Pembeli: " + order.getBuyerId().substring(0, 5) + "...");
        } else {
            holder.binding.tvBuyerName.setText("Hamba Allah");
        }

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            CartItem firstItem = order.getItems().get(0);

            holder.binding.tvOrderProductName.setText(firstItem.getName());
            holder.binding.tvDuration.setText("Durasi : " + firstItem.getDurationValue() + " " + firstItem.getDurationType());

            String prefixHarga = "Harga Per";
            if ("Harian".equalsIgnoreCase(firstItem.getDurationType())) prefixHarga = "Harga Perhari";
            else if ("Mingguan".equalsIgnoreCase(firstItem.getDurationType())) prefixHarga = "Harga Perminggu";
            else if ("Bulanan".equalsIgnoreCase(firstItem.getDurationType())) prefixHarga = "Harga Perbulan";

            holder.binding.tvDurationPrice.setText(prefixHarga + " : " + formatRupiah.format(firstItem.getPrice()));

            double itemSubTotal = firstItem.getPrice() * firstItem.getQuantity() * firstItem.getDurationValue();
            holder.binding.tvTotalPrice.setText(formatRupiah.format(order.getTotalAmount()));

            holder.binding.tvOrderQty.setText("x" + firstItem.getQuantity());

            Glide.with(context)
                    .load(firstItem.getImageUrl())
                    .centerCrop()
                    .into(holder.binding.imgOrderProduct);
        }

        holder.binding.tvOrderGrandTotal.setText(formatRupiah.format(order.getTotalAmount()));

        // 2. SETUP LOGIKA COUNTDOWN SLA (DUMB UI)
        if ("MENUNGGU KONFIRMASI".equals(order.getStatus()) && order.getSlaText() != null) {
            holder.binding.layoutAccessTime.setVisibility(View.VISIBLE);
            holder.binding.tvResponseDeadline.setText(order.getSlaText());

            // Tahan bentuk aslinya (radius sudut) agar tidak hilang menjadi block kotak
            holder.binding.layoutAccessTime.setBackgroundResource(R.drawable.bg_warning_text);

            if (order.isSlaUrgent()) {
                // Urgent: Background Merah Muda (#FFEBEE), Teks & Ikon Merah Tua (#D32F2F)
                holder.binding.layoutAccessTime.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
                holder.binding.tvResponseDeadline.setTextColor(Color.parseColor("#D32F2F"));
                holder.binding.icAccessTime.setColorFilter(Color.parseColor("#D32F2F"));
            } else {
                // Warning: Background Kuning Muda (#FFF3E0), Teks & Ikon Orange (#F57C00)
                holder.binding.layoutAccessTime.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                holder.binding.tvResponseDeadline.setTextColor(Color.parseColor("#F57C00"));
                holder.binding.icAccessTime.setColorFilter(Color.parseColor("#F57C00"));
            }
        } else {
            holder.binding.layoutAccessTime.setVisibility(View.GONE);
        }

        setupDynamicButton(holder, order);
    }

    private void setupDynamicButton(SellerOrderViewHolder holder, Order order) {
        holder.binding.btnOrderAction.setVisibility(View.GONE);
        holder.binding.btnReject.setVisibility(View.GONE);

        switch (order.getStatus()) {
            case "MENUNGGU KONFIRMASI":
                holder.binding.btnOrderAction.setVisibility(View.VISIBLE);
                holder.binding.btnReject.setVisibility(View.VISIBLE);
                holder.binding.btnOrderAction.setText("Terima Pesanan");
                holder.binding.btnOrderAction.setBackgroundTintList(context.getResources().getColorStateList(R.color.olive_500));
                holder.binding.btnOrderAction.setOnClickListener(v -> actionListener.onAcceptClicked(order));
                holder.binding.btnReject.setOnClickListener(v -> actionListener.onRejectClicked(order));
                break;

            case "DIPROSES":
                holder.binding.btnOrderAction.setVisibility(View.VISIBLE);
                holder.binding.btnOrderAction.setText("Kirim Pesanan");
                holder.binding.btnOrderAction.setBackgroundTintList(context.getResources().getColorStateList(R.color.gray_900));
                holder.binding.btnOrderAction.setOnClickListener(v -> actionListener.onAcceptClicked(order));
                break;

            case "DIKIRIM":
            case "SELESAI":
            case "DIBATALKAN":
            default:
                holder.binding.btnOrderAction.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class SellerOrderViewHolder extends RecyclerView.ViewHolder {
        ItemSellerOrderBinding binding;

        public SellerOrderViewHolder(@NonNull ItemSellerOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}