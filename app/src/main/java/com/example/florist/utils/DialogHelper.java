package com.example.florist.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;

import com.example.florist.R;

public class DialogHelper {

    /**
     * Membuat dan mengonfigurasi dasar Custom Dialog dengan animasi dan background transparan.
     */
    public static Dialog createCustomDialog(Context context, int layoutResId) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(layoutResId);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }

        dialog.setCancelable(true);
        return dialog;
    }
}