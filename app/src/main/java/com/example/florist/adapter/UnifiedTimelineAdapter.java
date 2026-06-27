package com.example.florist.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.model.TimelineEvent;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UnifiedTimelineAdapter extends RecyclerView.Adapter<UnifiedTimelineAdapter.ViewHolder> {

    private final List<TimelineEvent> events = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));

    // === VARIABEL PERSONALISASI ===
    private String storeName = "Penjual";
    private String buyerName = "Pembeli";
    private boolean isSellerMode = false;
    private final OnTimelineActionListener listener;

    public interface OnTimelineActionListener {
        void onQuoteClicked(TimelineEvent event);
        void onImageZoomClicked(String imageUrl);
    }

    public UnifiedTimelineAdapter(OnTimelineActionListener listener) {
        this.listener = listener;
    }

    public void setStoreName(String storeName) { this.storeName = storeName; notifyDataSetChanged(); }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; notifyDataSetChanged(); }
    public void setSellerMode(boolean isSellerMode) { this.isSellerMode = isSellerMode; notifyDataSetChanged(); }

    public void setEvents(List<TimelineEvent> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_unified_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimelineEvent event = events.get(position);

        // 1. FORMAT NAMA EKSPLISIT (Sesuai permintaan Anda)
        String formattedTitle = "";
        if (event.getEventType() == TimelineEvent.TYPE_COMPLAINT) {
            // Jika komplain, pastikan label " (Pelanggan)" ditambahkan
            String actor = buyerName + " (Pelanggan)";
            formattedTitle = "<b>" + actor + "</b> mengajukan <b>Komplain</b>";
        } else if (event.getEventType() == TimelineEvent.TYPE_RESOLUTION) {
            // Jika resolusi, gunakan nama toko
            String actor = storeName;
            formattedTitle = "<b>" + actor + "</b> melakukan <b>Perbaikan Komplain</b>";
        } else {
            // Jika perawatan rutin, gunakan nama toko
            String actor = storeName;
            formattedTitle = "<b>" + actor + "</b> melakukan <b>Perawatan Rutin</b>";
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            holder.tvTitle.setText(android.text.Html.fromHtml(formattedTitle, android.text.Html.FROM_HTML_MODE_COMPACT));
        } else {
            holder.tvTitle.setText(android.text.Html.fromHtml(formattedTitle));
        }

        // 2. Set Teks & Tanggal
        holder.tvDescription.setText(event.getDescription());
        if (event.getTimestamp() != null) {
            holder.tvDate.setText(sdf.format(event.getTimestamp().toDate()));
        }

        // 3. Set Gambar (Jika Ada)
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            holder.imgEvidence.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(event.getImageUrl()).into(holder.imgEvidence);

            // Klik Gambar untuk Zoom
            holder.imgEvidence.setOnClickListener(v -> {
                if (listener != null) listener.onImageZoomClicked(event.getImageUrl());
            });
        } else {
            holder.imgEvidence.setVisibility(View.GONE);
        }

        // 4. Styling Warna Otomatis
        int colorCode;
        if (event.getEventType() == TimelineEvent.TYPE_COMPLAINT) {
            colorCode = Color.parseColor("#E53935");
            holder.cardContent.setStrokeColor(Color.parseColor("#FFCDD2"));
            holder.cardContent.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            holder.btnQuote.setText("Balas Komplain ke Chat"); // Teks dinamis
        } else if (event.getEventType() == TimelineEvent.TYPE_RESOLUTION) {
            colorCode = Color.parseColor("#1E88E5");
            holder.cardContent.setStrokeColor(Color.parseColor("#BBDEFB"));
            holder.cardContent.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
            holder.btnQuote.setText("Kutip Perbaikan ke Chat"); // Teks dinamis
        } else {
            colorCode = Color.parseColor("#4CAF50");
            holder.cardContent.setStrokeColor(Color.parseColor("#E8F5E9"));
            holder.cardContent.setCardBackgroundColor(Color.WHITE);
            holder.btnQuote.setText("Kutip Perawatan ke Chat"); // Teks dinamis
        }

        holder.timelineIndicator.setImageTintList(ColorStateList.valueOf(colorCode));
        holder.timelineLineTop.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
//        holder.timelineLineBottom.setVisibility(position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);

        holder.btnQuote.setOnClickListener(v -> {
            if (listener != null) listener.onQuoteClicked(event);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }



    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvDescription;
        ImageView imgEvidence, timelineIndicator;
        View timelineLineTop, timelineLineBottom;
        MaterialCardView cardContent;
        androidx.appcompat.widget.AppCompatButton btnQuote;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            imgEvidence = itemView.findViewById(R.id.imgEvidence);
            timelineIndicator = itemView.findViewById(R.id.timelineIndicator);
            timelineLineTop = itemView.findViewById(R.id.timelineLineTop);
            timelineLineBottom = itemView.findViewById(R.id.timelineLineBottom);
            cardContent = itemView.findViewById(R.id.cardContent);
            btnQuote = itemView.findViewById(R.id.btnQuote);
        }
    }
    public int getPositionByEventId(String eventId) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getEventId().equals(eventId)) {
                return i;
            }
        }
        return -1; // Tidak ditemukan
    }
}