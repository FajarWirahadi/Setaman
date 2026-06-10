package com.example.florist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.databinding.ItemBuyerMaintenanceTestBinding;
import com.example.florist.model.MaintenanceLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuyerMaintenanceAdapter extends RecyclerView.Adapter<BuyerMaintenanceAdapter.ViewHolder> {

    private boolean isSellerMode = false;
    private final List<MaintenanceLog> logs = new ArrayList<>();
    private final OnLogActionListener listener;
    private String storeName = "Penjual";

    public interface OnLogActionListener {
        void onChatSellerClicked(MaintenanceLog log);
    }

    public void setSellerMode(boolean isSellerMode) {
        this.isSellerMode = isSellerMode;
    }

    public BuyerMaintenanceAdapter(OnLogActionListener listener) {
        this.listener = listener;
    }

    public void setLogs(List<MaintenanceLog> newLogs) {
        logs.clear();
        logs.addAll(newLogs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBuyerMaintenanceTestBinding binding = ItemBuyerMaintenanceTestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaintenanceLog log = logs.get(position);

        String message = "<b>" + storeName + "</b> telah melakukan <b>Perawatan Rutin</b>";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            holder.binding.tvActionTitle.setText(android.text.Html.fromHtml(message, android.text.Html.FROM_HTML_MODE_COMPACT));
        } else {
            holder.binding.tvActionTitle.setText(android.text.Html.fromHtml(message));
        }

        holder.binding.tvLogDescription.setText(log.getDescription());
        String imageUrl = logs.get(position).getImageUrl();

        holder.binding.imgLogPhoto.setOnClickListener(v -> {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                showZoomableImageDialog(holder.itemView.getContext(), imageUrl);
            }
        });

        if (log.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm", new Locale("id", "ID"));
            holder.binding.tvLogDate.setText(sdf.format(log.getCreatedAt().toDate()));
        }

        Glide.with(holder.itemView.getContext())
                .load(log.getImageUrl())
                .into(holder.binding.imgLogPhoto);

        if (log.getImageUrl() != null && !log.getImageUrl().isEmpty()) {
            holder.binding.imgLogPhoto.setVisibility(View.VISIBLE);
            holder.binding.btnChatSeller.setVisibility(View.VISIBLE);

            Glide.with(holder.itemView.getContext())
                    .load(log.getImageUrl())
                    .into(holder.binding.imgLogPhoto);
        } else {
            holder.binding.imgLogPhoto.setVisibility(View.GONE);
            holder.binding.btnChatSeller.setVisibility(View.GONE);
        }

        holder.binding.btnChatSeller.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatSellerClicked(log);
            }
        });
    }
    @Override
    public int getItemCount() { return logs.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemBuyerMaintenanceTestBinding binding;
        private String storeName = "Penjual";
        ViewHolder(ItemBuyerMaintenanceTestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

    }
    public void setStoreName(String storeName) {
        if (storeName != null && !storeName.isEmpty()) {
            this.storeName = storeName;
            notifyDataSetChanged();
        }
    }

    private void showZoomableImageDialog(android.content.Context context, String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_zoom_layout);

        com.github.chrisbanes.photoview.PhotoView photoView = dialog.findViewById(R.id.photoView);
        android.widget.ImageButton btnClose = dialog.findViewById(R.id.btnCloseZoom);

        com.bumptech.glide.Glide.with(context)
                .load(imageUrl)
                .into(photoView);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}