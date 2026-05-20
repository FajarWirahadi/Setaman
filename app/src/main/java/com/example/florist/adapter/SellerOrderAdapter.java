package com.example.florist.adapter;

import android.content.Context;
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

        holder.binding.tvOrderId.setText(order.getOrderId());
        holder.binding.tvOrderStatus.setText(order.getStatus());

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
            holder.binding.tvTotalPrice.setText(formatRupiah.format(itemSubTotal));

            holder.binding.tvOrderQty.setText("x" + firstItem.getQuantity());

            Glide.with(context)
                    .load(firstItem.getImageUrl())
                    .centerCrop()
                    .into(holder.binding.imgOrderProduct);
        }

        holder.binding.tvOrderGrandTotal.setText(formatRupiah.format(order.getTotalAmount()));

        setupDynamicButton(holder, order);
    }

    private void setupDynamicButton(SellerOrderViewHolder holder, Order order) {
        holder.binding.btnOrderAction.setVisibility(View.GONE);
        holder.binding.btnReject.setVisibility(View.GONE);

        switch (order.getStatus()) {
            case "Menunggu Konfirmasi":
                holder.binding.btnOrderAction.setVisibility(View.VISIBLE);
                holder.binding.btnReject.setVisibility(View.VISIBLE);
                holder.binding.btnOrderAction.setText("Terima Pesanan");
                holder.binding.btnOrderAction.setBackgroundTintList(context.getResources().getColorStateList(R.color.olive_500));
                holder.binding.btnOrderAction.setOnClickListener(v -> actionListener.onAcceptClicked(order));
                holder.binding.btnReject.setOnClickListener(v -> actionListener.onRejectClicked(order));
                break;

            case "Diproses":
                holder.binding.btnOrderAction.setVisibility(View.VISIBLE);
                holder.binding.btnOrderAction.setText("Kirim Pesanan");
                holder.binding.btnOrderAction.setBackgroundTintList(context.getResources().getColorStateList(R.color.gray_900));
                holder.binding.btnOrderAction.setOnClickListener(v -> actionListener.onAcceptClicked(order));
                break;

            case "Dikirim":
                holder.binding.btnOrderAction.setVisibility(View.GONE);
                break;

            case "Selesai":
            case "Dibatalkan":
                String reason = order.getCancellationReason();
                if (reason != null && !reason.isEmpty()) {
                    holder.binding.tvOrderStatus.setText("Dibatalkan: " + reason);
                } else {
                    holder.binding.tvOrderStatus.setText("Dibatalkan");
                }
                holder.binding.tvOrderStatus.setTextColor(context.getResources().getColor(R.color.text_error));
                break;
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