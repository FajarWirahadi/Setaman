package com.example.florist.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.florist.R;
import com.google.android.material.card.MaterialCardView; // WAJIB DI-IMPORT

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final List<Date> dates;
    private int selectedPosition = -1;
    private final OnDateClickListener listener;
    private final Set<String> routineDates = new HashSet<>();
    private final Set<String> complaintDates = new HashSet<>();

    public interface OnDateClickListener {
        void onDateClick(Date date);
    }

    public CalendarAdapter(List<Date> dates, int defaultSelectedPos, OnDateClickListener listener) {
        this.dates = dates;
        this.selectedPosition = defaultSelectedPos;
        this.listener = listener;
    }

    public void setTaskIndicators(List<String> routines, List<String> complaints) {
        routineDates.clear();
        if (routines != null) routineDates.addAll(routines);

        complaintDates.clear();
        if (complaints != null) complaintDates.addAll(complaints);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_date, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        Date date = dates.get(position);

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", new Locale("id", "ID"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", new Locale("id", "ID"));

        // [ENTERPRISE FIX]: Samakan locale pencocokan key dengan ViewModel
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String dateKey = keyFormat.format(date);

        holder.tvDay.setText(dayFormat.format(date));
        holder.tvDate.setText(dateFormat.format(date));

        // Nyalakan/Matikan Indikator Visual (Titik)
        holder.indicatorRoutine.setVisibility(routineDates.contains(dateKey) ? View.VISIBLE : View.GONE);
        holder.indicatorComplaint.setVisibility(complaintDates.contains(dateKey) ? View.VISIBLE : View.GONE);

        // ==============================================================
        // [ENTERPRISE FIX]: Gunakan API resmi MaterialCardView untuk Styling
        // ==============================================================
        MaterialCardView cardView = (MaterialCardView) holder.itemView;

        if (selectedPosition == position) {
            // Saat dipilih (Selected)
            cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.olive_500));
            cardView.setStrokeWidth(0); // Hilangkan garis batas abu-abu saat aktif

            holder.tvDay.setTextColor(Color.WHITE);
            holder.tvDate.setTextColor(Color.WHITE);
        } else {
            // Saat tidak dipilih (Unselected)
            cardView.setCardBackgroundColor(Color.WHITE);

            // Konversi 1dp ke pixel secara matematis untuk garis batas
            int strokePx = (int) (1 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            cardView.setStrokeWidth(strokePx);
            cardView.setStrokeColor(Color.parseColor("#E0E0E0"));

            holder.tvDay.setTextColor(Color.parseColor("#757575"));
            holder.tvDate.setTextColor(Color.BLACK);
        }
        // ==============================================================

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            int previousPos = selectedPosition;
            selectedPosition = currentPos;

            // Refresh hanya 2 item yang berubah agar ringan di memori
            notifyItemChanged(previousPos);
            notifyItemChanged(selectedPosition);

            listener.onDateClick(dates.get(currentPos));
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvDate;
        View indicatorRoutine, indicatorComplaint;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvDate = itemView.findViewById(R.id.tvDate);
            indicatorRoutine = itemView.findViewById(R.id.indicatorRoutine);
            indicatorComplaint = itemView.findViewById(R.id.indicatorComplaint);
        }
    }
}