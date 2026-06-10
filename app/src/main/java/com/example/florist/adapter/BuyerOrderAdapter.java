package com.example.florist.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

        String status = order.getStatus();

        if ("PENDING".equalsIgnoreCase(status)) {
            long waktuDibuat = order.getCreatedAt() != null ? order.getCreatedAt().toDate().getTime() : 0;
            long waktuSekarang = System.currentTimeMillis();
            long batasDuaPuluhEmpatJam = 24 * 60 * 60 * 1000;

            if (waktuSekarang - waktuDibuat > batasDuaPuluhEmpatJam) {
                holder.binding.tvOrderStatus.setText("⚠️ Batas Waktu Habis");
                holder.binding.tvOrderStatus.setTextColor(Color.parseColor("#757575"));
                holder.binding.tvOrderStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));

                holder.binding.btnOrderAction.setText("Pembayaran Kadaluarsa");
                holder.binding.btnOrderAction.setEnabled(false);
                holder.binding.btnOrderAction.setAlpha(0.5f);
                holder.binding.btnOrderAction.setOnClickListener(null);

            } else {
                holder.binding.tvOrderStatus.setText("Belum Bayar");
                holder.binding.tvOrderStatus.setTextColor(Color.parseColor("#D32F2F"));
                holder.binding.tvOrderStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));

                holder.binding.btnOrderAction.setText("Bayar Sekarang");
                holder.binding.btnOrderAction.setOnClickListener(v -> {
                    String token = order.getSnapToken();
                    if (token != null && !token.isEmpty() && !token.equals("ERROR_DARI_SERVER")) {
                        String snapUrl = "https://app.sandbox.midtrans.com/snap/v2/vtweb/" + token;
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(snapUrl));
                        context.startActivity(browserIntent);
                    } else {
                        android.widget.Toast.makeText(context, "Sistem gagal memuat token pembayaran.", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } else if ("PROCESSING".equalsIgnoreCase(status) || "Diproses".equalsIgnoreCase(status)) {
            holder.binding.tvOrderStatus.setText("Diproses");
            holder.binding.tvOrderStatus.setTextColor(Color.parseColor("#F57C00"));
            holder.binding.tvOrderStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));

            holder.binding.btnOrderAction.setText("Lihat Detail");
            holder.binding.btnOrderAction.setOnClickListener(v -> goToDetail(order.getOrderId()));

        } else if ("SHIPPED".equalsIgnoreCase(status) || "Dikirim".equalsIgnoreCase(status)) {
            holder.binding.tvOrderStatus.setText("Dikirim");

            holder.binding.btnOrderAction.setText("Terima Pesanan");
            holder.binding.btnOrderAction.setOnClickListener(v -> {
                holder.binding.btnOrderAction.setEnabled(false);
                holder.binding.btnOrderAction.setText("Memproses...");
                if (actionListener != null) {
                    actionListener.onAcceptOrder(order);
                }
            });

        } else if ("RENTED".equalsIgnoreCase(status) || "Disewa".equalsIgnoreCase(status)) {
            holder.binding.tvOrderStatus.setText("Dalam Perawatan");

            holder.binding.btnOrderAction.setText("Akhiri Masa Sewa");
            holder.binding.btnOrderAction.setOnClickListener(v -> {
                holder.binding.btnOrderAction.setEnabled(false);
                holder.binding.btnOrderAction.setText("Mengakhiri...");
                if (actionListener != null) {
                    actionListener.onEndRental(order);
                }
            });

        } else if ("Dibatalkan".equalsIgnoreCase(status)) {
            holder.binding.tvOrderStatus.setText("Dibatalkan");
            holder.binding.tvOrderStatus.setTextColor(Color.parseColor("#757575"));
            holder.binding.tvOrderStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));

            holder.binding.btnOrderAction.setText("Pesanan Dibatalkan");
            holder.binding.btnOrderAction.setEnabled(false);
            holder.binding.btnOrderAction.setAlpha(0.5f);

        } else if ("COMPLETED".equalsIgnoreCase(status) || "Selesai".equalsIgnoreCase(status)) {
            holder.binding.tvOrderStatus.setText("Selesai");
            holder.binding.tvOrderStatus.setTextColor(Color.parseColor("#388E3C"));
            holder.binding.tvOrderStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));

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