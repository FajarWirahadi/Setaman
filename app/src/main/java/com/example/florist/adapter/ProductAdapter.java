package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;
    private OnProductActionClickListener listener;

    public interface OnProductActionClickListener {
        void onEditClick(Product product);
        void onDeactivateClick(Product product);
        void onMenuClick(Product product, View view);
    }

    public ProductAdapter(Context context, List<Product> productList, OnProductActionClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    public void updateList(List<Product> newList) {
        this.productList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_owner, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvStock;
        ImageView imgProduct;
        Button btnEdit, btnDeactivate;
        ImageButton btnMenu;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvStock = itemView.findViewById(R.id.tvProductStock);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDeactivate = itemView.findViewById(R.id.btnDeactivate);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }

        public void bind(Product product) {
            tvName.setText(product.getName());
            tvStock.setText(String.valueOf(product.getStock()));

            NumberFormat formatRp = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            String price = formatRp.format(product.getPrice());

            if (price.endsWith(",00")) price = price.substring(0, price.length() - 3);
            tvPrice.setText(price + " /Hari");

            if (product.isActive()) {
                btnDeactivate.setText("Nonaktifkan");
                btnDeactivate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        context.getResources().getColor(R.color.red_600)
                ));
            } else {
                btnDeactivate.setText("Aktifkan");
                btnDeactivate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        context.getResources().getColor(R.color.olive_500)));
            }

            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.logo_icon)
                    .centerCrop()
                    .into(imgProduct);

            btnEdit.setOnClickListener(v -> {listener.onEditClick(product);});
            btnDeactivate.setOnClickListener(v -> {listener.onDeactivateClick(product);});
            btnMenu.setOnClickListener(v -> {listener.onMenuClick(product, btnMenu);});
        }
    }
}
