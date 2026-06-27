package com.example.florist.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemBuyerOrderBinding;
import com.example.florist.model.CartItem;
import com.example.florist.model.Order;
import com.example.florist.utils.Constants;
import com.example.florist.utils.StatusBadgeHelper;
import com.example.florist.views.buyer.BuyerOrderDetailActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuyerOrderAdapter extends RecyclerView.Adapter<BuyerOrderAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onAcceptOrder(Order order);
        void onEndRental(Order order);
    }

    private Context context;
    private List<Order> orderList = new ArrayList<>();
    private OnOrderActionListener actionListener;

    public BuyerOrderAdapter(Context context, OnOrderActionListener actionListener) {
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
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBuyerOrderBinding binding = ItemBuyerOrderBinding.inflate(LayoutInflater.from(context), parent, false);
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        holder.binding.tvOrderId.setText(order.getOrderId());

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        formatRupiah.setMaximumFractionDigits(0);

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            CartItem firstItem = order.getItems().get(0);
            holder.binding.tvOrderProductName.setText(firstItem.getName());

            String durationInfo = (firstItem.getDurationValue() != 0)
                    ? " x " + firstItem.getDurationValue() + " " + firstItem.getDurationType()
                    : "";
            holder.binding.tvOrderQty.setText("x" + firstItem.getQuantity());
            holder.binding.tvDuration.setText("Durasi : " + firstItem.getDurationValue() + " " + firstItem.getDurationType());

            String prefixHarga = "Harga Per";
            if ("Harian".equalsIgnoreCase(firstItem.getDurationType())) prefixHarga = "Harga Perhari";
            else if ("Mingguan".equalsIgnoreCase(firstItem.getDurationType())) prefixHarga = "Harga Perminggu";
            else if ("Bulanan".equalsIgnoreCase(firstItem.getDurationType())) prefixHarga = "Harga Perbulan";

            holder.binding.tvDurationPrice.setText(prefixHarga + " : " + formatRupiah.format(firstItem.getPrice()));
            holder.binding.tvTotalPrice.setText(formatRupiah.format(order.getTotalAmount()));


            Glide.with(context)
                    .load(firstItem.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.building)
                    .into(holder.binding.imgOrderProduct);

//            if (order.getItems().size() > 1) {
//                holder.binding.tvMoreItems.setVisibility(View.VISIBLE);
//                holder.binding.tvMoreItems.setText("+ " + (order.getItems().size() - 1) + " produk lainnya");
//            } else {
//                holder.binding.tvMoreItems.setVisibility(View.GONE);
//            }
        holder.binding.tvOrderGrandTotal.setText(formatRupiah.format(order.getTotalAmount()));
        }

        holder.binding.btnReviewAction.setVisibility(View.GONE);
        holder.binding.btnOrderAction.setEnabled(true);
        holder.binding.btnOrderAction.setAlpha(1.0f);

        String status = order.getStatus() != null ? order.getStatus().toUpperCase() : "";

        StatusBadgeHelper.applyStatus(context, holder.binding.tvOrderStatus, status);

        if (Constants.ORDER_PENDING.equals(status)) {
            long waktuDibuat = order.getCreatedAt() != null ? order.getCreatedAt().toDate().getTime() : 0;
            long batasDuaPuluhEmpatJam = 24 * 60 * 60 * 1000;

            if (System.currentTimeMillis() - waktuDibuat > batasDuaPuluhEmpatJam) {
                holder.binding.tvOrderStatus.setText("⚠️ KEDALUWARSA"); // Timpa teks khusus untuk expired
                holder.binding.btnOrderAction.setText("Pembayaran Kadaluarsa");
                holder.binding.btnOrderAction.setEnabled(false);
                holder.binding.btnOrderAction.setAlpha(0.5f);
            } else {
                holder.binding.btnOrderAction.setText("Bayar Sekarang");
                holder.binding.btnOrderAction.setOnClickListener(v -> {
                    String token = order.getSnapToken();
                    if (token != null && !token.isEmpty() && !token.equals("ERROR_DARI_SERVER")) {
                        String snapUrl = "https://app.sandbox.midtrans.com/snap/v2/vtweb/" + token;
                        context.startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(snapUrl)));
                    } else {
                        android.widget.Toast.makeText(context, "Gagal memuat token.", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else if (Constants.ORDER_WAITING.equals(status) || com.example.florist.utils.Constants.ORDER_PROCESSING.equals(status)) {
            holder.binding.btnOrderAction.setText("Lihat Detail");
            holder.binding.btnOrderAction.setOnClickListener(v -> goToDetail(order.getOrderId()));

        } else if (Constants.ORDER_SHIPPED.equals(status)) {
            holder.binding.btnOrderAction.setText("Terima Pesanan");
            holder.binding.btnOrderAction.setOnClickListener(v -> {
                v.setEnabled(false);
                ((android.widget.Button) v).setText("Memproses...");
                if (actionListener != null) actionListener.onAcceptOrder(order);
                v.postDelayed(() -> { if (v != null) { v.setEnabled(true); ((android.widget.Button) v).setText("Terima Pesanan"); } }, 3000);
            });

        } else if (Constants.RENTAL_ACTIVE.equals(status)) {
            holder.binding.btnOrderAction.setText("Akhiri Masa Sewa");
            holder.binding.btnOrderAction.setOnClickListener(v -> {
                holder.binding.btnOrderAction.setEnabled(false);
                holder.binding.btnOrderAction.setText("Mengakhiri...");
                if (actionListener != null) actionListener.onEndRental(order);
            });

        } else if (Constants.ORDER_CANCELED.equals(status)) {
            holder.binding.btnOrderAction.setText("Pesanan Dibatalkan");
            holder.binding.btnOrderAction.setEnabled(false);
            holder.binding.btnOrderAction.setAlpha(0.5f);

        } else if (Constants.ORDER_COMPLETED.equals(status)) {
            holder.binding.btnOrderAction.setText("Lihat Detail");
            holder.binding.btnOrderAction.setOnClickListener(v -> goToDetail(order.getOrderId()));

            if (!order.isReviewed()) {
                holder.binding.btnReviewAction.setVisibility(View.VISIBLE);
                holder.binding.btnReviewAction.setOnClickListener(v -> {
                    Intent intent = new Intent(context, com.example.florist.views.buyer.AddReviewActivity.class);
                    intent.putExtra("EXTRA_ORDER_ID", order.getOrderId());

                    if (order.getItems() != null && !order.getItems().isEmpty()) {
                        CartItem firstItem = order.getItems().get(0);
                        intent.putExtra("EXTRA_PRODUCT_ID", firstItem.getProductId());
                        intent.putExtra("EXTRA_PRODUCT_NAME", firstItem.getName());
                        intent.putExtra("EXTRA_PRODUCT_IMAGE", firstItem.getImageUrl());
                    }
                    context.startActivity(intent);
                });
            }
        }
    }

    private void goToDetail(String orderId) {
        Intent intent = new Intent(context, BuyerOrderDetailActivity.class);
        intent.putExtra("EXTRA_ORDER_ID", orderId);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        ItemBuyerOrderBinding binding;
        public OrderViewHolder(@NonNull ItemBuyerOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}