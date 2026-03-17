package com.example.florist.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.zhihu.matisse.engine.ImageEngine;

public class GlideEngine implements ImageEngine {

    @Override
    public void loadThumbnail(Context context, int resize, Drawable placeholder, ImageView imageView, Uri uri) {
        Glide.with(context)
                .asBitmap()
                .load(uri)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)
                        .override(resize, resize).placeholder(placeholder).centerCrop().skipMemoryCache(true))
//                .apply(RequestOptions.skipMemoryCacheOf(true))
                .into(imageView);
    }

    @Override
    public void loadGifThumbnail(Context context, int resize, Drawable placeholder, ImageView imageView, Uri uri) {
        Glide.with(context)
                .asBitmap()
                .load(uri)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)
                        .override(resize, resize).placeholder(placeholder).centerCrop().skipMemoryCache(true))
//                .apply(RequestOptions.skipMemoryCacheOf(true))
                .into(imageView);
    }

    @Override
    public void loadImage(Context context, int resizeX, int resizeY, ImageView imageView, Uri uri) {
        Glide.with(context)
                .load(uri)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)
                        .override(resizeX, resizeY).priority(Priority.HIGH).fitCenter().skipMemoryCache(true))
//                .apply(RequestOptions.skipMemoryCacheOf(true))
                .into(imageView);
    }

    @Override
    public void loadGifImage(Context context, int resizeX, int resizeY, ImageView imageView, Uri uri) {
        Glide.with(context)
                .asGif()
                .load(uri)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)
                        .override(resizeX, resizeY).priority(Priority.HIGH).fitCenter().skipMemoryCache(true))
//                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
//                .apply(RequestOptions.skipMemoryCacheOf(true))
                .into(imageView);
    }

    @Override
    public boolean supportAnimatedGif() {
        return true;
    }
}