package com.example.florist.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

public class NetworkMonitor extends LiveData<Boolean> {

    private final ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback networkCallback;
    private final Context applicationContext;

    public NetworkMonitor(Context context) {
        applicationContext = context.getApplicationContext();
        connectivityManager = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                postValue(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                postValue(false);
            }
        };
    }

    @Override
    protected void onActive() {
        super.onActive();
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);

        postValue(NetworkUtils.isNetworkAvailable(applicationContext));
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }
}