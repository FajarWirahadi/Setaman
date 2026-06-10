package com.example.florist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;

import java.util.ArrayList;
import java.util.List;

public class ShopSuggestionAdapter extends RecyclerView.Adapter<ShopSuggestionAdapter.ViewHolder> {

    // Inner Model Class
    public static class ShopItem {
        public String shopId;
        public String shopName;
        public String shopImageUrl;
        public ShopItem(String shopId, String shopName, String shopImageUrl) {
            this.shopId = shopId;
            this.shopName = shopName;
            this.shopImageUrl = shopImageUrl;
        }
    }

    private List<ShopItem> shopList = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onShopClick(String shopId);
    }

    public ShopSuggestionAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<ShopItem> newList) {
        shopList.clear();
        shopList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShopItem item = shopList.get(position);
        holder.tvShopName.setText(item.shopName);
        Glide.with(holder.itemView.getContext())
                .load(item.shopImageUrl)
                .placeholder(R.drawable.building)
                .circleCrop()
                .into(holder.imgShop);


        holder.itemView.setOnClickListener(v -> listener.onShopClick(item.shopId));
    }

    @Override
    public int getItemCount() {
        return shopList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvShopName;
        ImageView imgShop;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvShopName = itemView.findViewById(R.id.tvSuggestedShopName);
            imgShop = itemView.findViewById(R.id.imgSuggestedShop);
        }
    }
}