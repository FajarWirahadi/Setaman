package com.example.florist.adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;

import java.util.List;

public class BuyerImageSliderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // KELAS HELPER UNTUK MEMBEDAKAN TIPE MEDIA
    public static class SliderItem {
        public static final int TYPE_IMAGE = 0;
        public static final int TYPE_VIDEO = 1;

        public String url;
        public int type;

        public SliderItem(String url, int type) {
            this.url = url;
            this.type = type;
        }
    }

    private Context context;
    private List<SliderItem> sliderItems;

    public BuyerImageSliderAdapter(Context context, List<SliderItem> sliderItems) {
        this.context = context;
        this.sliderItems = sliderItems;
    }

    // Menentukan wajah mana yang akan dipakai
    @Override
    public int getItemViewType(int position) {
        return sliderItems.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == SliderItem.TYPE_VIDEO) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_slider_video, parent, false);
            return new VideoViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_slider_image, parent, false);
            return new ImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SliderItem item = sliderItems.get(position);

        if (holder.getItemViewType() == SliderItem.TYPE_VIDEO) {
            // LOGIKA PEMUTAR VIDEO
            VideoViewHolder videoHolder = (VideoViewHolder) holder;
            videoHolder.videoView.setVideoPath(item.url);
            videoHolder.videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true); // Putar berulang-ulang
                videoHolder.videoView.start(); // Putar otomatis
            });
        } else {
            // LOGIKA PENAMPIL GAMBAR
            ImageViewHolder imageHolder = (ImageViewHolder) holder;
            Glide.with(context)
                    .load(item.url)
                    .placeholder(R.drawable.logo_icon)
                    .centerCrop()
                    .into(imageHolder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return sliderItems != null ? sliderItems.size() : 0;
    }

    // Wajah 1: Pemegang Gambar
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgSlider);
        }
    }

    // Wajah 2: Pemegang Video
    static class VideoViewHolder extends RecyclerView.ViewHolder {
        VideoView videoView;
        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoSlider);
        }
    }
}