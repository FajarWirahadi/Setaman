package com.example.florist.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Product;
import com.example.florist.repository.ProductRepository;

import java.util.HashMap;
import java.util.List;

public class ProductViewModel extends ViewModel {
    private ProductRepository repository;
    private MutableLiveData<List<Product>> allProducts = new MutableLiveData<>();
    private MutableLiveData<HashMap<String, ProductRepository.ShopData>> shopDataMap = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isSuccess = new MutableLiveData<>();

    public ProductViewModel() {
        repository = new ProductRepository();
    }

    public LiveData<List<Product>> getAllProducts() {return allProducts;}
    public LiveData<HashMap<String, ProductRepository.ShopData>> getShopDataMap() {return shopDataMap;}
    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<String> getErrorMessage() {return errorMessage;}
    public LiveData<Boolean> getIsSuccess() {return isSuccess;}


    public void addProduct(Product product, Uri imageUri) {
        isLoading.setValue(true);

        repository.addProduct(product, imageUri, new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                isSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }


    public void updateProductMultiple(Product product, List<Uri> newImages, List<String> oldImages) {
        isLoading.setValue(true);

        repository.updateProductWithMultipleImages(product, newImages, oldImages, new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                isSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void fetchAllProducts() {
        isLoading.setValue(true);
        repository.getAllProducts(new ProductRepository.ProductListCallback() {
            @Override
            public void onSuccess(List<Product> product) {
                isLoading.setValue(false);
                allProducts.setValue(product);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }
    public void fetchShopNames() {
        isLoading.setValue(true);
        repository.getShopNames(new ProductRepository.ShopNamesCallback() {
            @Override
            public void onSuccess(java.util.HashMap<String, ProductRepository.ShopData> shopNames) {
                isLoading.setValue(false);
                shopDataMap.setValue(shopNames);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal memuat nama toko: " + message);
            }
        });
    }

}
