package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemCartBinding;
import com.example.florist.model.CartItem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder>{
    private Context context;
    private List<CartItem> cartList;
    private CartClickListener listener;

    public interface CartClickListener {
        void onPlusClick(CartItem item, int position);
        void onMinusClick(CartItem item, int position);
        void onDeleteClick(CartItem item, int position);
        void onEditDurationClick(CartItem item, int position);
    }

    public CartAdapter(Context context, List<CartItem> cartList, CartClickListener listener) {
        this.context = context;
        this.cartList = cartList;
        this.listener = listener;
    }
    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(LayoutInflater.from(context), parent, false);
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartAdapter.CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);

        String rentLabel =+ item.getDurationValue() + " " + item.getDurationType();
        if (item.getShopName() != null && !item.getShopName().isEmpty()) {
            holder.binding.tvCartShopName.setText(item.getShopName());
        }
        holder.binding.tvCartName.setText(item.getName());
        holder.binding.tvDurationLabel.setText(rentLabel);

        // 2. Set Jumlah
        holder.binding.tvQuantity.setText(String.valueOf(item.getQuantity()));

        // 3. Kalkulasi Sub-Total Harga Item Ini
        int multiplier = 1;
        if ("Mingguan".equals(item.getDurationType())) multiplier = 7;
        else if ("Bulanan".equals(item.getDurationType())) multiplier = 30;

        long subTotal = (long) item.getPrice() * item.getQuantity() * item.getDurationValue() * multiplier;

        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        formatRupiah.setMaximumFractionDigits(0);
        holder.binding.tvCartPrice.setText(formatRupiah.format(subTotal));

        // 4. Set Gambar
        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.logo_icon) // Ganti jika kau punya ikon placeholder lain
                .centerCrop()
                .into(holder.binding.imgCartProduct);

        // 5. Pasang Telinga untuk Tombol-Tombol
        holder.binding.btnPlus.setOnClickListener(v -> listener.onPlusClick(item, position));
        holder.binding.btnMinus.setOnClickListener(v -> listener.onMinusClick(item, position));
        holder.binding.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item, position));
        holder.binding.btnEditDuration.setOnClickListener(v-> listener.onEditDurationClick(item, position));
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    public class CartViewHolder extends RecyclerView.ViewHolder {
        ItemCartBinding binding;

        public CartViewHolder(@NonNull ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
