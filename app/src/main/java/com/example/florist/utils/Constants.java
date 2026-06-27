package com.example.florist.utils;

public class Constants {
    // 1. STATUS PESANAN (Tabel orders)
    public static final String ORDER_PENDING = "PENDING"; // Sebelum dibayar di Midtrans
    public static final String ORDER_WAITING = "MENUNGGU KONFIRMASI";
    public static final String ORDER_PROCESSING = "DIPROSES";
    public static final String ORDER_SHIPPED = "DIKIRIM";
    public static final String ORDER_COMPLETED = "SELESAI";
    public static final String ORDER_CANCELED = "DIBATALKAN";

    // 2. STATUS MASA SEWA (Tabel rentals)
    public static final String RENTAL_ACTIVE = "SEWA AKTIF";
    public static final String RENTAL_WAITING_PULL = "MENUNGGU PENARIKAN";
    public static final String RENTAL_COMPLETED = "SELESAI";

    // 3. STATUS KOMPLAIN (Tabel complaints)
    public static final String COMPLAINT_WAITING_RESPONSE = "MENUNGGU RESPON";
    public static final String COMPLAINT_PROCESSING = "PROSES PERBAIKAN";
    public static final String COMPLAINT_WAITING_CONFIRM = "MENUNGGU KONFIRMASI";
    public static final String COMPLAINT_MANDATORY_VISIT = "KUNJUNGAN WAJIB";
    public static final String COMPLAINT_DISPUTE = "DISPUTE ADMIN";
    public static final String COMPLAINT_COMPLETED = "SELESAI";

    // 4. STATUS PERPANJANGAN (Tabel extensions)
    public static final String EXT_PENDING = "PENDING";
    public static final String EXT_PAID = "LUNAS";
    public static final String EXT_CANCELED = "DIBATALKAN/EXPIRED";
}