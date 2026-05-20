package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.florist.databinding.ItemSellerComplaintBinding;
import com.example.florist.model.Order;
import java.util.ArrayList;
import java.util.List;

public class SellerComplaintAdapter extends RecyclerView.Adapter<SellerComplaintAdapter.ViewHolder> {

    private final Context context;
    private final List<Order> complaintOrders = new ArrayList<>();
    private final OnComplaintClickListener listener;

    public interface OnComplaintClickListener {
        void onRespondClicked(Order order);
    }

    public SellerComplaintAdapter(Context context, OnComplaintClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<Order> newList) {
        complaintOrders.clear();
        complaintOrders.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSellerComplaintBinding binding = ItemSellerComplaintBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = complaintOrders.get(position);

        holder.binding.tvOrderId.setText("ID Pesanan: #" + order.getOrderId());
        holder.binding.tvBuyerName.setText("Pembeli: " + (order.getReceiverName() != null ? order.getReceiverName() : "Pelanggan"));

        holder.binding.btnRespond.setOnClickListener(v -> listener.onRespondClicked(order));
    }

    @Override
    public int getItemCount() { return complaintOrders.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ItemSellerComplaintBinding binding;
        public ViewHolder(ItemSellerComplaintBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}