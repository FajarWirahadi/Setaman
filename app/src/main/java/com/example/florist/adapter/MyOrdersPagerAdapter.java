package com.example.florist.adapter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.florist.views.buyer.OrderListFragment;

public class MyOrdersPagerAdapter extends FragmentStateAdapter {

    private final String[] statuses = {"Menunggu Konfirmasi", "Diproses", "Dikirim", "Selesai", "Dibatalkan"};

    public MyOrdersPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putString("ORDER_STATUS", statuses[position]);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return statuses.length;
    }
}