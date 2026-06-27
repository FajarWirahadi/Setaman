package com.example.florist.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import com.example.florist.R;

public class StatusBadgeHelper {

    public static void applyStatus(Context context, TextView textView, String status) {
        if (status == null || textView == null) return;

        int textColorRes;
        int iconRes;

        // 1. Tentukan Warna Teks Solid & Ikon Berdasarkan Status
        switch (status.toUpperCase()) {
            // 🟡 KELOMPOK PENDING (Kuning / Oranye)
            case Constants.ORDER_PENDING:
            case Constants.ORDER_WAITING: // Ini JUGA mencakup COMPLAINT_WAITING_CONFIRM karena teksnya sama
                textColorRes = R.color.text_warning;
                iconRes = R.drawable.ic_status_waiting;
                break;
            case Constants.COMPLAINT_WAITING_RESPONSE:
                textColorRes = R.color.text_warning;
                iconRes = R.drawable.ic_status_chat;
                break;

            // 🔵 KELOMPOK PROSES (Biru / Main Color)
            case Constants.ORDER_PROCESSING:
            case Constants.COMPLAINT_PROCESSING:
                textColorRes = R.color.blue_500; // Mengikuti penyesuaian Anda
                iconRes = R.drawable.ic_status_processing;
                break;
            case Constants.ORDER_SHIPPED:
                textColorRes = R.color.main_color;
                iconRes = R.drawable.ic_status_shipped;
                break;

            // 🟢 KELOMPOK BERHASIL (Hijau)
            case Constants.ORDER_COMPLETED:
            case Constants.EXT_PAID:
                textColorRes = R.color.olive_500;
                iconRes = R.drawable.ic_status_completed;
                break;
            case Constants.RENTAL_ACTIVE:
                textColorRes = R.color.olive_500;
                iconRes = R.drawable.ic_status_active;
                break;

            // 🟣 KELOMPOK PENARIKAN (Ungu / Indigo)
            case Constants.RENTAL_WAITING_PULL:
                textColorRes = R.color.purple_200; // Mengikuti penyesuaian Anda
                iconRes = R.drawable.ic_status_return;
                break;

            // 🔴 KELOMPOK BATAL & BERMASALAH (Merah)
            case Constants.ORDER_CANCELED:
            case Constants.EXT_CANCELED:
                textColorRes = R.color.red_500;
                iconRes = R.drawable.ic_status_canceled;
                break;
            case Constants.COMPLAINT_MANDATORY_VISIT:
                textColorRes = R.color.red_500;
                iconRes = R.drawable.ic_status_visit;
                break;
            case Constants.COMPLAINT_DISPUTE:
                textColorRes = R.color.red_500;
                iconRes = R.drawable.ic_status_dispute;
                break;

            // ⚪ DEFAULT (Abu-abu)
            default:
                textColorRes = R.color.gray_700;
                iconRes = 0; // Tanpa ikon
                break;
        }

        // 2. Ekstraksi Warna Solid
        int solidColor = ContextCompat.getColor(context, textColorRes);

        // 3. Kalkulasi Background Otomatis (15% Opacity dari Warna Solid)
        int pastelBackgroundColor = ColorUtils.setAlphaComponent(solidColor, 38);

        // 4. Eksekusi Rendering UI (Menggunakan Capitalize Each Word)
        String cleanStatus = status.replace("_", " ");
        textView.setText(capitalizeEachWord(cleanStatus));
        textView.setTextColor(solidColor);

        textView.setBackgroundResource(R.drawable.bg_status_badge);
        textView.setBackgroundTintList(ColorStateList.valueOf(pastelBackgroundColor));

        // 5. Injeksi Ikon secara dinamis
        if (iconRes != 0) {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
            int paddingPx = (int) (4 * context.getResources().getDisplayMetrics().density);
            textView.setCompoundDrawablePadding(paddingPx);
            textView.setCompoundDrawableTintList(ColorStateList.valueOf(solidColor));
        } else {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            textView.setCompoundDrawablePadding(0);
        }
    }

    /**
     * Fungsi Helper untuk mengubah teks menjadi Capitalize Each Word
     * Contoh: "MENUNGGU KONFIRMASI" -> "Menunggu Konfirmasi"
     */
    private static String capitalizeEachWord(String text) {
        if (text == null || text.isEmpty()) return text;

        String[] words = text.toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }
        return result.toString().trim();
    }
}