package com.example.florist.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemAgendaTaskBinding;
import com.example.florist.model.MaintenanceTaskUIModel;
import com.example.florist.model.Rental;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceScheduleAdapter extends RecyclerView.Adapter<MaintenanceScheduleAdapter.MaintenanceViewHolder>{

    private final Context context;
    private final List<MaintenanceTaskUIModel> taskList = new ArrayList<>();
    private final OnMaintenanceListener listener;

    public interface OnMaintenanceListener{
        void onAddLogClicked(MaintenanceTaskUIModel task);

        void onCardClicked(Rental rental);
        void onChatClicked(Rental rental);
    }

    public MaintenanceScheduleAdapter(Context context, OnMaintenanceListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<MaintenanceTaskUIModel> newTasks) {
        taskList.clear();
        taskList.addAll(newTasks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MaintenanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAgendaTaskBinding binding = ItemAgendaTaskBinding.inflate(LayoutInflater.from(context), parent, false);
        return new MaintenanceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MaintenanceViewHolder holder, int position) {
        MaintenanceTaskUIModel task = taskList.get(position);
        Rental rental = task.rental;

        holder.binding.tvOrderId.setText(task.displayOrderId);
        holder.binding.tvPlantName.setText(rental.getPlantName());
        holder.binding.tvBuyerName.setText(rental.getBuyerName() != null ? rental.getBuyerName() : "Nama Pembeli");
        holder.binding.tvDuration.setText(task.displayDuration);
        holder.binding.tvNextMaintenance.setText(task.displayNextDate);

        holder.binding.tvTaskStatus.setText(task.taskStatusText);

        int solidColor = androidx.core.content.ContextCompat.getColor(context, task.statusColorCode);

        int backgroundColor = androidx.core.graphics.ColorUtils.setAlphaComponent(solidColor, 38);

        holder.binding.tvTaskStatus.setBackgroundResource(R.drawable.bg_status_badge);

        holder.binding.tvTaskStatus.setTextColor(solidColor);
        holder.binding.tvTaskStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(backgroundColor));
        holder.binding.tvNextMaintenance.setTextColor(solidColor);
        // ==============================================================

        holder.binding.btnCompleteTask.setVisibility(task.showCompleteButton ? View.VISIBLE : View.GONE);
        holder.binding.btnCompleteTask.setText(task.buttonText);

        if (rental.getPlantImageUrl() != null && !rental.getPlantImageUrl().isEmpty()) {
            Glide.with(context).load(rental.getPlantImageUrl()).centerCrop().into(holder.binding.imgPlant);
        }

        holder.binding.btnCompleteTask.setOnClickListener(v -> listener.onAddLogClicked(task));

        String fullAddress = rental.getFullDeliveryAddress();
        holder.binding.tvAddress.setText(fullAddress);

        if (rental.getDeliveryAddress() != null) {
            holder.binding.layoutAddress.setVisibility(View.VISIBLE);
            holder.binding.layoutAddress.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(fullAddress));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(mapIntent);
                } else {
                    Uri browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(fullAddress));
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
                    context.startActivity(browserIntent);
                }
            });
        } else {
            holder.binding.layoutAddress.setVisibility(View.GONE);
        }

        holder.binding.btnChat.setOnClickListener(v -> listener.onChatClicked(rental));
        holder.binding.getRoot().setOnClickListener(v -> listener.onCardClicked(rental));
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class MaintenanceViewHolder extends RecyclerView.ViewHolder {
        ItemAgendaTaskBinding binding;
        public MaintenanceViewHolder(@NonNull ItemAgendaTaskBinding binding) {
            super (binding.getRoot());
            this.binding = binding;
        }
    }
}