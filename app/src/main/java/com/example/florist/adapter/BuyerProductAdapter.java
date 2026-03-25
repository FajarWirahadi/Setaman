package com.example.florist.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemProductGridBinding;
import com.example.florist.model.Product;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BuyerProductAdapter extends RecyclerView.Adapter<BuyerProductAdapter.BuyerViewHolder> {
    private Context context;
    private List<Product> productList;
    private OnItemClickListener listener;

    private boolean isGrid;

    public interface OnItemClickListener {
        void onProductClick(Product product);
    }
    public BuyerProductAdapter(Context context, List<Product> productlist, boolean isGrid, OnItemClickListener listener) {
        this.context = context;
        this.productList = productlist;
        this.listener = listener;
        this.isGrid = isGrid;
    }

    @NonNull
    @Override
    public BuyerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductGridBinding binding = ItemProductGridBinding.inflate(LayoutInflater.from(context), parent, false);

        ViewGroup.LayoutParams layoutParams = binding.getRoot().getLayoutParams();
        if (isGrid) {
            layoutParams.width = (int) (160 * context.getResources().getDisplayMetrics().density);
        } else {
            layoutParams.width = (int) (130 * context.getResources().getDisplayMetrics().density);
        }
        binding.getRoot().setLayoutParams(layoutParams);

        return new BuyerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BuyerViewHolder holder, int position) {
        holder.bind(productList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void updateList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    class BuyerViewHolder extends RecyclerView.ViewHolder {

        private final ItemProductGridBinding binding;

        BuyerViewHolder(ItemProductGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("SetTextI18n")
        public void bind(Product product, OnItemClickListener listener) {
            binding.tvProductName.setText(product.getName());
            binding.tvRating.setText(String.valueOf(product.getRating()) + " |");
            binding.tvSold.setText(product.getRentCount() + " disewa");
            binding.tvCategoryTag.setText(product.getCategory());

            NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            binding.tvPrice.setText(formatRupiah.format(product.getPrice()));

            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.logo_icon)
                    .centerCrop()
                    .into(binding.imgProduct);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }
    }
}
