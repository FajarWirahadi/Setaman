package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.databinding.ItemBuyerOrderBinding;
import com.example.florist.model.CartItem;
import com.example.florist.model.Order;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orderList = new ArrayList<>();

    public OrderAdapter(Context context) {
        this.context = context;
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
        holder.binding.tvOrderStatus.setText(order.getStatus());

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            CartItem firstItem = order.getItems().get(0);
            holder.binding.tvOrderProductName.setText(firstItem.getName());
            holder.binding.tvOrderQty.setText(firstItem.getQuantity() + " barang x " + firstItem.getDurationValue() + " " + firstItem.getDurationType());

            Glide.with(context)
                    .load(firstItem.getImageUrl())
                    .centerCrop()
                    .into(holder.binding.imgOrderProduct);

            if (order.getItems().size() > 1) {
                holder.binding.tvMoreItems.setVisibility(View.VISIBLE);
                holder.binding.tvMoreItems.setText("+ " + (order.getItems().size() - 1) + " produk lainnya");
            } else {
                holder.binding.tvMoreItems.setVisibility(View.GONE);
            }
        }

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);
        holder.binding.tvOrderGrandTotal.setText(formatRupiah.format(order.getTotalAmount()));

        String status = order.getStatus();

        if ("PENDING".equalsIgnoreCase(status)) {
            holder.binding.btnOrderAction.setText("Bayar Sekarang");

            holder.binding.btnOrderAction.setOnClickListener(v -> {
                String token = order.getSnapToken();
                if (token != null && !token.isEmpty() && !token.equals("ERROR_DARI_SERVER")) {
                    String snapUrl = "https://app.sandbox.midtrans.com/snap/v2/vtweb/" + token;
                    android.content.Intent browserIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(snapUrl));
                    context.startActivity(browserIntent);
                } else {
                    android.widget.Toast.makeText(context, "Sistem gagal memuat token pembayaran.", android.widget.Toast.LENGTH_SHORT).show();
                }
            });

        } else if ("Dikirim".equalsIgnoreCase(status)) {
            holder.binding.btnOrderAction.setText("Terima Pesanan");
            // Biarkan background hijau

            holder.binding.btnOrderAction.setOnClickListener(v -> {
                // TODO: Update status ke 'Selesai' di Firestore
                android.widget.Toast.makeText(context, "Fitur konfirmasi terima pesanan belum aktif", android.widget.Toast.LENGTH_SHORT).show();
            });

        } else if ("Dibatalkan".equalsIgnoreCase(status)) {
            holder.binding.btnOrderAction.setText("Pesanan Dibatalkan");
            holder.binding.btnOrderAction.setEnabled(false);
            holder.binding.btnOrderAction.setAlpha(0.5f);
        }else {
            holder.binding.btnOrderAction.setText("Lihat Detail");
            holder.binding.btnOrderAction.setEnabled(true);
            holder.binding.btnOrderAction.setAlpha(1.0f);
            holder.binding.btnOrderAction.setOnClickListener(v -> {
                // TODO: Buka OrderDetailActivity
                android.widget.Toast.makeText(context, "Membuka detail pesanan...", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
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