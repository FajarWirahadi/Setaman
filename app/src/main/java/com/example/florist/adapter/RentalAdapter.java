package com.example.florist.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemRentalBinding;
import com.example.florist.model.Rental;
import com.example.florist.views.seller.RentalDetailActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RentalAdapter extends RecyclerView.Adapter<RentalAdapter.RentalViewHolder> {

    private final Context context;
    private final List<Rental> rentalList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

    public RentalAdapter(Context context) {
        this.context = context;
    }

    public void updateData(List<Rental> newRentals) {
        this.rentalList.clear();
        this.rentalList.addAll(newRentals);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RentalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRentalBinding binding = ItemRentalBinding.inflate(LayoutInflater.from(context), parent, false);
        return new RentalViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RentalViewHolder holder, int position) {
        Rental rental = rentalList.get(position);

        holder.binding.tvStoreName.setText(rental.getSellerName() != null ? rental.getSellerName() : "Toko Setaman");
        holder.binding.tvPlantName.setText(rental.getPlantName());

        Glide.with(context)
                .load(rental.getPlantImageUrl())
                .centerCrop()
                .placeholder(R.drawable.building)
                .into(holder.binding.imgPlant);

        String status = rental.getStatus() != null ? rental.getStatus() : "AKTIF";
        String statusUpper = status.toUpperCase();

        if ("AKTIF".equals(statusUpper) || "DISEWA".equals(statusUpper)) {
            holder.binding.tvRentalStatus.setText("Sewa Aktif");
            holder.binding.tvRentalStatus.setTextColor(ContextCompat.getColor(context, R.color.green_600));
            holder.binding.tvRentalStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green_50)));

        } else if ("SELESAI".equals(statusUpper)) {
            holder.binding.tvRentalStatus.setText("Selesai");
            holder.binding.tvRentalStatus.setTextColor(ContextCompat.getColor(context, R.color.gray_600)); // Anggap Anda punya gray_600 di colors.xml
            holder.binding.tvRentalStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_100)));

        } else if ("KOMPLAIN".equals(statusUpper) || "PENDING".equals(statusUpper) || "PROSES PERBAIKAN".equals(statusUpper)) {
            holder.binding.tvRentalStatus.setText(status);
            holder.binding.tvRentalStatus.setTextColor(ContextCompat.getColor(context, R.color.red_500));
            holder.binding.tvRentalStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_50)));

        } else if ("MENUNGGU KONFIRMASI".equals(statusUpper)) {
            holder.binding.tvRentalStatus.setText(status);
            holder.binding.tvRentalStatus.setTextColor(ContextCompat.getColor(context, R.color.yellow_600));
            holder.binding.tvRentalStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.yellow_50)));

        } else {
            // Fallback default
            holder.binding.tvRentalStatus.setText(status);
            holder.binding.tvRentalStatus.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.binding.tvRentalStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_200)));
        }
        
        String start = rental.getStartDate() != null ? dateFormat.format(rental.getStartDate().toDate()) : "-";
        String end = rental.getEndDate() != null ? dateFormat.format(rental.getEndDate().toDate()) : "-";

        if (rental.getStartDate() != null && ("AKTIF".equals(statusUpper) || "DISEWA".equals(statusUpper))) {
            Date nextDate = calculateNextMaintenanceDate(rental.getStartDate().toDate());
            
            Calendar todayCal =  Calendar.getInstance();
            Calendar nextCal =  Calendar.getInstance();
            nextCal.setTime(nextDate);

            boolean isToday = todayCal.get(Calendar.YEAR) == nextCal.get(Calendar.YEAR) &&
                    todayCal.get(Calendar.DAY_OF_YEAR) == nextCal.get(Calendar.DAY_OF_YEAR);

            if (isToday) {
                holder.binding.tvRentalPeriod.setText("Jadwal Perawatan: HARI INI 💧");
                holder.binding.tvRentalPeriod.setTextColor(ContextCompat.getColor(context, R.color.main_color));
                holder.binding.tvRentalPeriod.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.binding.tvRentalPeriod.setText("Perawatan Berikutnya: " + dateFormat.format(nextDate));
                holder.binding.tvRentalPeriod.setTextColor(ContextCompat.getColor(context, R.color.gray_500));
                holder.binding.tvRentalPeriod.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        } else {
            // Jika sudah selesai atau komplain, tampilkan masa sewa biasa
            holder.binding.tvRentalPeriod.setText("Periode: " + start + " - " + end);
            holder.binding.tvRentalPeriod.setTextColor(ContextCompat.getColor(context, R.color.gray_500));
            holder.binding.tvRentalPeriod.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        holder.binding.btnViewMaintenance.setOnClickListener(v -> {
            Intent intent = new Intent(context, RentalDetailActivity.class);
            intent.putExtra("RENTAL_ID", rental.getRentalId());
            intent.putExtra("ORDER_ID", rental.getOrderId());
            intent.putExtra("ROLE", "BUYER");
            context.startActivity(intent);
        });
    }

    private  Date calculateNextMaintenanceDate( Date startDate) {
         Calendar nextCal =  Calendar.getInstance();
        nextCal.setTime(startDate);
        nextCal.set( Calendar.HOUR_OF_DAY, 0); nextCal.set( Calendar.MINUTE, 0); nextCal.set( Calendar.SECOND, 0); nextCal.set( Calendar.MILLISECOND, 0);

         Calendar todayCal =  Calendar.getInstance();
        todayCal.set( Calendar.HOUR_OF_DAY, 0); todayCal.set( Calendar.MINUTE, 0); todayCal.set( Calendar.SECOND, 0); todayCal.set( Calendar.MILLISECOND, 0);

        int interval = 3;
        nextCal.add( Calendar.DAY_OF_YEAR, interval);
        while (nextCal.before(todayCal)) {
            nextCal.add( Calendar.DAY_OF_YEAR, interval);
        }
        return nextCal.getTime();
    }
    @Override
    public int getItemCount() {
        return rentalList.size();
    }

    public static class RentalViewHolder extends RecyclerView.ViewHolder {
        ItemRentalBinding binding;
        public RentalViewHolder(@NonNull ItemRentalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}