package com.example.florist.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemAgendaTaskBinding;
import com.example.florist.model.Rental;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaintenanceScheduleAdapter extends RecyclerView.Adapter<MaintenanceScheduleAdapter.MaintenanceViewHolder>{

    private final Context context;
    private final List<Rental> rentalList = new ArrayList<>();
    private final OnMaintenanceListener listener;

    public interface OnMaintenanceListener{
        void onAddLogClicked(Rental rental);
        void onCardClicked(Rental rental);
        void onChatClicked(Rental rental);
    }

    public MaintenanceScheduleAdapter(Context context, OnMaintenanceListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<Rental> newRentals) {
        rentalList.clear();
        rentalList.addAll(newRentals);
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
        Rental rental = rentalList.get(position);

        String shortId = rental.getRentalId().length() > 8 ? rental.getRentalId().substring(0, 8) + "..." : rental.getRentalId();
        holder.binding.tvOrderId.setText("Sewa ID: " + shortId);
        holder.binding.tvPlantName.setText(rental.getPlantName());
        holder.binding.tvBuyerName.setText(rental.getBuyerName() != null ? rental.getBuyerName() : "Nama Pembeli");

        if (rental.getPlantImageUrl() != null && !rental.getPlantImageUrl().isEmpty()) {
            Glide.with(context).load(rental.getPlantImageUrl()).centerCrop().into(holder.binding.imgPlant);
        }

        // --- TANGGAL & DURASI ---
        if (rental.getStartDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
            String startDateStr = sdf.format(rental.getStartDate().toDate());
            String endDateStr = rental.getEndDate() != null ? sdf.format(rental.getEndDate().toDate()) : "-";
            holder.binding.tvDuration.setText("Masa Sewa: " + startDateStr + " s/d " + endDateStr);

            Date startDate = rental.getStartDate().toDate();
            Date nextDate = calculateNextMaintenanceDate(startDate);
            holder.binding.tvNextMaintenance.setText("Berikutnya: " + sdf.format(nextDate));
        }

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

        boolean isOverdue = false;
        if (rental.getStartDate() != null && "AKTIF".equalsIgnoreCase(rental.getStatus())) {
            long diffMillis = System.currentTimeMillis() - rental.getStartDate().toDate().getTime();
            long diffDays = diffMillis / (1000 * 60 * 60 * 24);
            long lastScheduledDay = diffDays - (diffDays % 3);

            Calendar lastScheduledCal = Calendar.getInstance();
            lastScheduledCal.setTime(rental.getStartDate().toDate());
            lastScheduledCal.add(Calendar.DAY_OF_YEAR, (int) lastScheduledDay);

            long actualLastMaintTime = rental.getLastMaintenanceDate() != null ?
                    rental.getLastMaintenanceDate().toDate().getTime() : rental.getStartDate().toDate().getTime();

            if (actualLastMaintTime < lastScheduledCal.getTimeInMillis()) {
                isOverdue = true;
            }
        }

        if ("SELESAI".equalsIgnoreCase(rental.getStatus())) {
            holder.binding.btnCompleteTask.setText("Selesai");
            holder.binding.tvTaskStatus.setTextColor(context.getResources().getColor(R.color.gray_600));
            holder.binding.tvTaskStatus.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.parseColor("#F8FAFC")));
            holder.binding.btnCompleteTask.setVisibility(View.GONE);

        } else if ("PROSES PERBAIKAN".equalsIgnoreCase(rental.getStatus()) || "Komplain".equalsIgnoreCase(rental.getStatus())) {
            holder.binding.tvTaskStatus.setText("Tugas: Perbaikan Komplain!");
            holder.binding.tvTaskStatus.setTextColor(android.graphics.Color.WHITE);
            holder.binding.tvTaskStatus.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.parseColor("#E53935"))); // Merah Terang

            holder.binding.tvNextMaintenance.setText("SEGERA LAKUKAN KUNJUNGAN");
            holder.binding.btnCompleteTask.setText("Kirim Bukti Perbaikan");
            holder.binding.btnCompleteTask.setVisibility(View.VISIBLE);
            holder.binding.btnCompleteTask.setBackgroundTintList(context.getResources().getColorStateList(R.color.red_500));
            holder.binding.btnCompleteTask.setOnClickListener(v -> listener.onAddLogClicked(rental));

        } else if (isOverdue) {
            holder.binding.tvTaskStatus.setText("JADWAL TERLEWAT!");
            holder.binding.tvTaskStatus.setTextColor(android.graphics.Color.WHITE);
            holder.binding.tvTaskStatus.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.parseColor("#F57C00")));

            holder.binding.tvNextMaintenance.setText("Tugas Kemarin yang Belum Diselesaikan");
            holder.binding.tvNextMaintenance.setTextColor(android.graphics.Color.parseColor("#F57C00"));

            holder.binding.btnCompleteTask.setText("Kirim Bukti Kunjungan");
            holder.binding.btnCompleteTask.setVisibility(View.VISIBLE);
            holder.binding.btnCompleteTask.setBackgroundTintList(context.getResources().getColorStateList(R.color.olive_500));
            holder.binding.btnCompleteTask.setOnClickListener(v -> listener.onAddLogClicked(rental));

        } else {
            holder.binding.btnCompleteTask.setText("Kirim Bukti");
            holder.binding.btnCompleteTask.setVisibility(View.VISIBLE);
            holder.binding.btnCompleteTask.setBackgroundTintList(context.getResources().getColorStateList(R.color.olive_500));
            holder.binding.btnCompleteTask.setOnClickListener(v -> listener.onAddLogClicked(rental));

            holder.binding.tvTaskStatus.setText("Tugas Hari Ini");
            holder.binding.tvTaskStatus.setTextColor(context.getResources().getColor(R.color.main_color));
            holder.binding.tvTaskStatus.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9")));
        }

        holder.binding.btnChat.setOnClickListener(v -> listener.onChatClicked(rental));
        holder.binding.getRoot().setOnClickListener(v -> listener.onCardClicked(rental));
    }

    private Date calculateNextMaintenanceDate(Date startDate) {
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(startDate);
        nextCal.set(Calendar.HOUR_OF_DAY, 0); nextCal.set(Calendar.MINUTE, 0); nextCal.set(Calendar.SECOND, 0); nextCal.set(Calendar.MILLISECOND, 0);

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0); todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0);

        int interval = 3;
        nextCal.add(Calendar.DAY_OF_YEAR, interval);
        while (nextCal.before(todayCal)) {
            nextCal.add(Calendar.DAY_OF_YEAR, interval);
        }
        return nextCal.getTime();
    }

    @Override
    public int getItemCount() {
        return rentalList.size();
    }

    public static class MaintenanceViewHolder extends RecyclerView.ViewHolder {
        ItemAgendaTaskBinding binding;
        public MaintenanceViewHolder(@NonNull ItemAgendaTaskBinding binding) {
            super (binding.getRoot());
            this.binding = binding;
        }
    }
}