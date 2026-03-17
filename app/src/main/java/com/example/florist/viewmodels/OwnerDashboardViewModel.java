package com.example.florist.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.AuthRepository;
import com.example.florist.model.ProductRepository;
import com.example.florist.model.Shop;
import com.example.florist.model.ShopRepository;
import com.example.florist.views.seller.OwnerDashboardActivity;
import com.google.firebase.auth.FirebaseUser;

public class OwnerDashboardViewModel extends ViewModel {
    private ShopRepository shopRepository;
    private AuthRepository authRepoitory;
    private ProductRepository productRepository;

    private MutableLiveData<Shop> shopData = new MutableLiveData<>();
    private MutableLiveData<String> updateImageSuccess = new MutableLiveData<>();
    private MutableLiveData<Integer> totalProducts = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public OwnerDashboardViewModel() {
        shopRepository = new ShopRepository();
        productRepository = new ProductRepository();
        authRepoitory = AuthRepository.getInstance();
    }

    public LiveData<Shop> getShopData() {return shopData;}
    public LiveData<Integer> getTotalProducts() {return totalProducts;}
    public LiveData<String> getUpdateImageSuccess() {return updateImageSuccess;}
    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<String> getErrorMessage() {return errorMessage;}

    public void loadDashboardData() {
        isLoading.setValue(true);
        FirebaseUser user = authRepoitory.getCurrentUser();

        if (user != null ) {
            String shopId = user.getUid();

            shopRepository.getShopData(shopId, new ShopRepository.ShopDataCallback() {
                @Override
                public void onDataLoaded(Shop shop) {
                    isLoading.setValue(false);
                    shopData.setValue(shop);
                }

                @Override
                public void onError(String message) {
                    isLoading.setValue(false);
                    errorMessage.setValue(message);
                }
            });
        } else {
            isLoading.setValue(false);
            errorMessage.setValue("User tidak ditemukan. Silahkan login ulang");
        }

    }

    public void uploadNewProfileImage(Uri imageUri) {
        isLoading.setValue(true);

        shopRepository.updateShopImage(imageUri, new ShopRepository.UpdateImageCallback() {
            @Override
            public void onSuccess(String newImageUrl) {
                isLoading.setValue(false);
                updateImageSuccess.setValue(newImageUrl);
                loadDashboardData();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void loadTotalProducts() {
        FirebaseUser user = authRepoitory.getCurrentUser();
        if (user != null) {
            String ownerId = user.getUid();

            productRepository.getProductCountByOwner(ownerId, new ProductRepository.ProductCountCallback() {
                @Override
                public void onSuccess(int count) {
                    totalProducts.setValue(count);
                }

                @Override
                public void onError(String message) {
                    errorMessage.setValue(message);
                }
            });
        }
    }
}
