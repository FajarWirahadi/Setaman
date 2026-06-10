package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;

import java.util.ArrayList;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static final int TYPE_MEDIA = 1;
    private static final int TYPE_ADD_BUTTON = 2;
    private final Context context;
    private final List<Object> mediaList;
    private final int maxSelection;

    // Interface agar Activity tahu kapan tombol tambah diklik
    public interface OnItemClickListener {
        void onAddClick();
        void onDeleteClick(Object mediaItem);
        void onPreviewClick(Object mediaItem);
    }
    private final OnItemClickListener listener;

    public MediaAdapter(Context context, int maxSelection, OnItemClickListener listener) {
        this.context = context;
        this.mediaList = new ArrayList<>();
        this.maxSelection = maxSelection;
        this.listener = listener;
    }

    // Fungsi untuk update data dari Activity
    public void appendMediaList(List<Object> newMediaList) {
//        this.mediaList.clear();
        this.mediaList.clear(); // Hapus data lama di adapter
        if (newMediaList != null) {
            this.mediaList.addAll(newMediaList); // Masukkan semua data dari ViewModel
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mediaList.size() && mediaList.size() < maxSelection) {
            return TYPE_ADD_BUTTON;
        } else {
            return TYPE_MEDIA;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD_BUTTON) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_add_product, parent, false);
            return new AddButtonViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_media, parent, false);
            return new MediaViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_MEDIA){
            MediaViewHolder mediaHolder = (MediaViewHolder) holder;
            Object mediaUri = mediaList.get(position);

            // Glide otomatis bisa membuat thumbnail dari Video maupun Gambar
            Glide.with(context)
                    .load(mediaUri)
                    .centerCrop()
                    .into(mediaHolder.ivThumbnail);

            mediaHolder.ivDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(mediaUri);
                }
            });

            mediaHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPreviewClick(mediaUri);
                }
            });
        } else {
            AddButtonViewHolder addHolder = (AddButtonViewHolder) holder;
            int currentSize = mediaList.size();

            String countText = "" + currentSize;
            addHolder.tvCount.setText(countText);
            // Set listener klik pada item ini
            addHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddClick();
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        if (mediaList.size() < maxSelection) {
            return mediaList.size()+1;
        } else {
            return mediaList.size();
        }
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        ImageView ivDelete;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            // Pastikan ID ini sesuai dengan yang ada di item_media.xml
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }

    public static class AddButtonViewHolder extends RecyclerView.ViewHolder {
        TextView tvCount;
        public AddButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCount = itemView.findViewById(R.id.currentCountMedia);
        }
    }
}
