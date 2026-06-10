package com.example.florist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.florist.databinding.ItemTrackingTimelineBinding;
import com.example.florist.model.DeliveryLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrackingAdapter extends RecyclerView.Adapter<TrackingAdapter.TrackingViewHolder> {

    private final List<DeliveryLog> trackingList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));

    public void setTrackingData(List<DeliveryLog> logs) {
        this.trackingList.clear();
        this.trackingList.addAll(logs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrackingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrackingTimelineBinding binding = ItemTrackingTimelineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TrackingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackingViewHolder holder, int position) {
        DeliveryLog log = trackingList.get(position);
        holder.bind(log, position == trackingList.size() - 1);
    }

    @Override
    public int getItemCount() {
        return trackingList.size();
    }

    class TrackingViewHolder extends RecyclerView.ViewHolder {
        private final ItemTrackingTimelineBinding binding;

        public TrackingViewHolder(ItemTrackingTimelineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DeliveryLog log, boolean isLastItem) {
            binding.tvStatusTitle.setText(log.getStatusTitle());

            if (log.getCreatedAt() != null) {
                binding.tvStatusTime.setText(dateFormat.format(log.getCreatedAt().toDate()));
            }

            if (log.getDescription() != null && !log.getDescription().trim().isEmpty()) {
                binding.tvStatusNote.setText(log.getDescription());
                binding.tvStatusNote.setVisibility(View.VISIBLE);
            } else {
                binding.tvStatusNote.setVisibility(View.GONE);
            }
            if (isLastItem) {
                binding.timelineLine.setVisibility(View.INVISIBLE);
            } else {
                binding.timelineLine.setVisibility(View.VISIBLE);
            }
        }
    }
}