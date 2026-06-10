package com.example.florist.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.florist.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final List<Date> dates;
    private int selectedPosition = -1;
    private final OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(Date date);
    }

    public CalendarAdapter(List<Date> dates, int defaultSelectedPos, OnDateClickListener listener) {
        this.dates = dates;
        this.selectedPosition = defaultSelectedPos;
        this.listener = listener;
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

        holder.tvDay.setText(dayFormat.format(date));
        holder.tvDate.setText(dateFormat.format(date));

        if (selectedPosition == position) {
            holder.itemView.setBackgroundResource(R.drawable.rounded_success_button);
            holder.tvDay.setTextColor(Color.WHITE);
            holder.tvDate.setTextColor(Color.WHITE);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            holder.tvDay.setTextColor(Color.parseColor("#757575"));
            holder.tvDate.setTextColor(Color.BLACK);
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            int previousPos = selectedPosition;
            selectedPosition = currentPos;

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
        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}