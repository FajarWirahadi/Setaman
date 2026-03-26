package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemCheckoutBinding;
import com.example.florist.model.CartItem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.CheckoutViewHolder> {

    private Context context;
    private List<CartItem> checkoutList;

    public CheckoutAdapter(Context context, List<CartItem> checkoutList) {
        this.context = context;
        this.checkoutList = checkoutList;
    }

    @NonNull
    @Override
    public CheckoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCheckoutBinding binding = ItemCheckoutBinding.inflate(LayoutInflater.from(context), parent, false);
        return new CheckoutViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckoutViewHolder holder, int position) {
        CartItem item = checkoutList.get(position);
        if (item.getShopName() != null && !item.getShopName().isEmpty()) {
            holder.binding.tvCheckoutShopName.setText(item.getShopName());
        }
        holder.binding.tvCheckoutName.setText(item.getName());

        holder.binding.tvCheckoutDuration.setText("Durasi: " + item.getDurationValue() + " " + item.getDurationType());

        holder.binding.tvCheckoutQty.setText(item.getQuantity() + "x");

        int multiplier = 1;
        if ("Mingguan".equals(item.getDurationType())) multiplier = 7;
        else if ("Bulanan".equals(item.getDurationType())) multiplier = 30;

        long subTotal = (long) item.getPrice() * item.getQuantity() * item.getDurationValue() * multiplier;

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);
        holder.binding.tvCheckoutPrice.setText(formatRupiah.format(subTotal));

        // Set Gambar
        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.logo_icon)
                .centerCrop()
                .into(holder.binding.imgCheckoutProduct);
    }

    @Override
    public int getItemCount() {
        return checkoutList.size();
    }

    public static class CheckoutViewHolder extends RecyclerView.ViewHolder {
        ItemCheckoutBinding binding;

        public CheckoutViewHolder(@NonNull ItemCheckoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}