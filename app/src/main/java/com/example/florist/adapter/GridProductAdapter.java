package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.model.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GridProductAdapter extends RecyclerView.Adapter<GridProductAdapter.GridViewHolder> {

    private Context context;
    private List<Product> productList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public GridProductAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.productList = new ArrayList<>();
        this.listener = listener;
    }

    public void setProductList(List<Product> list) {
        this.productList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_grid, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvName.setText(product.getName());
        holder.tvCategory.setText(product.getCategory() != null ? product.getCategory() : "Tanaman");

        // Format Harga
        NumberFormat formatRp = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        String price = formatRp.format(product.getPrice());
        if (price.endsWith(",00")) price = price.substring(0, price.length() - 3);
        holder.tvPrice.setText(price + "/hari");

        // Dummy Rating/Terjual (Karena belum ada di Model Product)
        holder.tvRatingSold.setText("4.8 | 10+ tersewa");

        // Load Gambar Cover
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.logo_icon)
                    .into(holder.imgProduct);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(product));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class GridViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvPrice, tvRatingSold;
        ImageView imgProduct;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvCategory = itemView.findViewById(R.id.tvCategoryTag);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvRatingSold = itemView.findViewById(R.id.tvRatingSold);
            imgProduct = itemView.findViewById(R.id.imgProduct);
        }
    }
}