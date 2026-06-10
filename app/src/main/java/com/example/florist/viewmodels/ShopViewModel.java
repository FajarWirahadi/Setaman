package com.example.florist.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.repository.ShopRepository;

public class ShopViewModel extends ViewModel {
    private ShopRepository shopRepository;
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();

    public ShopViewModel() {
        shopRepository = new ShopRepository();
    }

    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<String> getErrorMessage() {return errorMessage;}
    public LiveData<Boolean> getIsSuccess() {return isSuccess;}

    public void createShop(String name, String address, Uri imageUri) {
        isLoading.setValue(true);

        shopRepository.createShop(name, address, imageUri, new ShopRepository.ShopCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(true);
                isSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }
}
