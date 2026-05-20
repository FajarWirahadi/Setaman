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
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;

public class OwnerDashboardViewModel extends ViewModel {
    private ShopRepository shopRepository;
    private AuthRepository authRepository;
    private ProductRepository productRepository;

    private MutableLiveData<Integer> countUnpaid = new MutableLiveData<>();
    private MutableLiveData<Integer> countProcessing = new MutableLiveData<>();
    private MutableLiveData<Integer> countShipped = new MutableLiveData<>();
    private MutableLiveData<Integer> countRented = new MutableLiveData<>();
    private MutableLiveData<Integer> countMaintenance = new MutableLiveData<>();
    private MutableLiveData<Integer> countComplaint = new MutableLiveData<>();
    private MutableLiveData<Shop> shopData = new MutableLiveData<>();
    private MutableLiveData<String> updateImageSuccess = new MutableLiveData<>();
    private MutableLiveData<Integer> totalProducts = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public OwnerDashboardViewModel() {
        shopRepository = new ShopRepository();
        productRepository = new ProductRepository();
        authRepository = AuthRepository.getInstance();
    }

    public LiveData<Integer> getCountUnpaid() {return countUnpaid;}
    public LiveData<Integer> getCountProcessing() {return countProcessing;}
    public LiveData<Integer> getCountShipped() {return countShipped;}
    public LiveData<Integer> getCountRented() {return countRented;}
    public LiveData<Integer> getCountMaintenance() {return countMaintenance;}
    public LiveData<Integer> getCountComplaint() {return countComplaint;}
    public LiveData<Shop> getShopData() {return shopData;}
    public LiveData<Integer> getTotalProducts() {return totalProducts;}
    public LiveData<String> getUpdateImageSuccess() {return updateImageSuccess;}
    public LiveData<Boolean> getIsLoading() {return isLoading;}
    public LiveData<String> getErrorMessage() {return errorMessage;}

    public void loadDashboardData() {
        isLoading.setValue(true);
        FirebaseUser user = authRepository.getCurrentUser();

        if (user != null ) {
            String shopId = user.getUid();

            shopRepository.getShopById(shopId, new ShopRepository.ShopDataCallback() {
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
        FirebaseUser user = authRepository.getCurrentUser();
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

    public void loadSellerOrderCounts() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) return;

        String currentSellerId = user.getUid();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Menunggu Konfirmasi")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countUnpaid.setValue((int) task.getResult().getCount());
                });
        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Diproses")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countProcessing.setValue((int) task.getResult().getCount());
                });

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Dikirim")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countShipped.setValue((int) task.getResult().getCount());
                });

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Selesai")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countRented.setValue((int) task.getResult().getCount());
                });

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Dalam Perawatan")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countMaintenance.setValue((int) task.getResult().getCount());
                });

        firestore.collection("orders")
                .whereEqualTo("sellerId", currentSellerId)
                .whereEqualTo("status", "Komplain")
                .count()
                .get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) countComplaint.setValue((int) task.getResult().getCount());
                });

    }
}
