package com.example.florist.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.florist.R;
import com.example.florist.databinding.ItemSellerOrderBinding;
import com.example.florist.model.CartItem;
import com.example.florist.model.Order;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaintenanceScheduleAdapter extends RecyclerView.Adapter<MaintenanceScheduleAdapter.MaintenanceViewHolder>{
    private final Context context;
    private final List<Order> orderList = new ArrayList<>();
    private final OnMaintenanceListener listener;

    public interface OnMaintenanceListener{
        void onAddLogClicked(Order order);
    }

    public MaintenanceScheduleAdapter(Context context, OnMaintenanceListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<Order> newOrders) {
        orderList.clear();
        orderList.addAll(newOrders);
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public MaintenanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSellerOrderBinding binding = ItemSellerOrderBinding.inflate(LayoutInflater.from(context), parent, false);
        return new MaintenanceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MaintenanceViewHolder holder, int position) {
        Order order = orderList.get(position);

        holder.binding.tvOrderId.setText(order.getOrderId());
        holder.binding.tvBuyerName.setText(order.getReceiverName() != null ? order.getReceiverName() : "Nama Pembeli");

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            CartItem item = order.getItems().get(0);
            holder.binding.tvOrderProductName.setText(item.getName());
        }

        if (order.getCreatedAt() != null) {
            Date startDate = order.getCreatedAt().toDate();
            Date nextDate = calculateNextMaintenanceDate(startDate);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MM yyyy", new Locale("id", "ID"));
            String nextDateString = sdf.format(nextDate);

            holder.binding.tvDuration.setText("Jadwal Perawatan Selanjutnya:");
            holder.binding.tvDuration.setTextColor(context.getResources().getColor(R.color.gray_700));

            holder.binding.tvDurationPrice.setText(nextDateString);
            holder.binding.tvDurationPrice.setTextColor(context.getResources().getColor(R.color.text_error));
            holder.binding.tvDurationPrice.setTypeface(null, Typeface.BOLD);
        }
        holder.binding.btnOrderAction.setText("Kirim Bukti");
        holder.binding.btnOrderAction.setVisibility(View.VISIBLE);
        holder.binding.btnReject.setVisibility(View.GONE);

        holder.binding.btnOrderAction.setOnClickListener(v -> listener.onAddLogClicked(order));
    }

    private Date calculateNextMaintenanceDate(Date startDate) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);

        Calendar todayCal = Calendar.getInstance();

        if (todayCal.before(startCal)) {
            startCal.add(Calendar.DAY_OF_YEAR, 7);
            return startCal.getTime();
        }

        long diffMillis = todayCal.getTimeInMillis() - startCal.getTimeInMillis();
        long diffDays = diffMillis / (24 * 60 * 60 * 1000);

        int interval = 7;
        long daysUntilNext = interval - (diffDays % interval);

        if (daysUntilNext == 7) {
            daysUntilNext = 0;
        }

        todayCal.add(Calendar.DAY_OF_YEAR, (int) daysUntilNext);
        return todayCal.getTime();
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class MaintenanceViewHolder extends RecyclerView.ViewHolder {
        ItemSellerOrderBinding binding;

        public MaintenanceViewHolder(@NonNull ItemSellerOrderBinding binding) {
            super (binding.getRoot());
            this.binding = binding;
        }
    }
}
