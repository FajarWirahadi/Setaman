package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Product;
import com.example.florist.model.ProductRepository;
import com.example.florist.model.Shop;
import com.example.florist.model.ShopRepository;

import java.util.List;

public class ShopProfileViewModel extends ViewModel {
    private final ShopRepository shopRepo = new ShopRepository();
    private final ProductRepository productRepo = new ProductRepository();

    private final MutableLiveData<Shop> shopData = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> shopProducts = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<Shop> getShopData() { return shopData; }
    public LiveData<List<Product>> getShopProducts() { return shopProducts; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadShopProfile(String shopId) {
        isLoading.setValue(true);

        // 1. Ambil Info Toko
        shopRepo.getShopById(shopId, new ShopRepository.ShopDataCallback() {
            @Override
            public void onDataLoaded(Shop shop) {
                shopData.setValue(shop);
                // Setelah toko didapat, langsung cari produknya!
                loadProducts(shopId);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal memuat profil toko: " + message);
            }
        });
    }

    private void loadProducts(String shopId) {
        productRepo.getProductsByOwner(shopId, new ProductRepository.ProductListCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                isLoading.setValue(false);
                shopProducts.setValue(products);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal memuat daftar produk: " + message);
            }
        });
    }
}