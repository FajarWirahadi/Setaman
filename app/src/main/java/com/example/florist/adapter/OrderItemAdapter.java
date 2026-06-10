package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemOrderDetailProductBinding;
import com.example.florist.model.CartItem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {

    private final Context context;
    private final List<CartItem> itemList;
    private final NumberFormat formatRupiah;

    public OrderItemAdapter(Context context, List<CartItem> itemList) {
        this.context = context;
        this.itemList = itemList;

        formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderDetailProductBinding binding = ItemOrderDetailProductBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = itemList.get(position);

        holder.binding.tvProductName.setText(item.getName());
        holder.binding.tvProductQtyDuration.setText(item.getQuantity() + "x (" + item.getDurationValue() + " " + item.getDurationType() + ")");

        double subtotal = item.getPrice() * item.getQuantity();
        holder.binding.tvProductPrice.setText(formatRupiah.format(subtotal));

        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.rounded_gray_layout)
                .centerCrop()
                .into(holder.binding.imgProduct);
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemOrderDetailProductBinding binding;

        public ViewHolder(ItemOrderDetailProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}