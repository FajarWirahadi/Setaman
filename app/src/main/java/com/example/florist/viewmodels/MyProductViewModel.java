package com.example.florist.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.AuthRepository;
import com.example.florist.model.Product;
import com.example.florist.model.ProductRepository;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MyProductViewModel extends ViewModel {
    private ProductRepository productRepository;
    private AuthRepository authRepository;

    private List<Product> allMyProduct = new ArrayList<>();
    private String currentSearchQuery = "";

    private MutableLiveData<List<Product>> filteredProducts = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();
    private MutableLiveData<Boolean> updateStatusSuceess = new MutableLiveData<>();


    private MutableLiveData<List<Product>> activeProducts = new MutableLiveData<>();
    private MutableLiveData<List<Product>> soldProducts = new MutableLiveData<>();

    private MutableLiveData<List<Product>> inactiveProducts = new MutableLiveData<>();
    private MutableLiveData <Integer> countActive = new MutableLiveData<>();
    private MutableLiveData <Integer> countSold = new MutableLiveData<>();
    private MutableLiveData <Integer> countInactive = new MutableLiveData<>();








    public MyProductViewModel() {
        productRepository = new ProductRepository();
        authRepository = AuthRepository.getInstance();
    }

    public ProductRepository getProductRepository() {return productRepository;}

    public AuthRepository getAuthRepository() {return authRepository;}

    public List<Product> getAllMyProduct() {return allMyProduct;}

    public MutableLiveData<List<Product>> getFilteredProducts() {return filteredProducts;}

    public MutableLiveData<Boolean> getIsLoading() {return isLoading;}

    public MutableLiveData<String> getErrorMessage() {return errorMessage;}

    public MutableLiveData<List<Product>> getActiveProducts() {return activeProducts;}

    public MutableLiveData<List<Product>> getSoldProducts() {return soldProducts;}

    public MutableLiveData<List<Product>> getInactiveProducts() {return inactiveProducts;}

    public MutableLiveData<Integer> getCountActive() {return countActive;}

    public MutableLiveData<Integer> getCountSold() {return countSold;}

    public MutableLiveData<Integer> getCountInactive() {return countInactive;}
    public MutableLiveData<Boolean> getDeleteSuccess() {return deleteSuccess;}

    public MutableLiveData<Boolean> getUpdateStatusSuceess() {return updateStatusSuceess;}

    public void fetchMyProducts() {
        isLoading.setValue(true);
        FirebaseUser user = authRepository.getCurrentUser();

        if (user == null) {
            errorMessage.setValue("User tidak valid, silahkan login ulang");
            isLoading.setValue(false);
            return;
        }

        productRepository.getProductsByOwner(user.getUid(), new ProductRepository.ProductListcallback() {
            @Override
            public void onSuccess(List<Product> product) {
                isLoading.setValue(false);
                allMyProduct = product;
                applyFilters();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void searchProduct(String query) {
        this.currentSearchQuery = query.toLowerCase().trim();
        applyFilters();
    }

    private void applyFilters() {
        List<Product> active = new ArrayList<>();
        List<Product> soldOut = new ArrayList<>();
        List<Product> inactive = new ArrayList<>();

        for (Product p : allMyProduct) {

            if (!p.getName().toLowerCase().contains(currentSearchQuery)) {
                continue; // Jika tidak cocok, lompati produk ini!
            }

            if (!p.isActive()) inactive.add(p);
            else if (p.getStock() <= 0) soldOut.add(p);
            else active.add(p);
        }

        activeProducts.setValue(active);
        soldProducts.setValue(soldOut);
        inactiveProducts.setValue(inactive);

        countActive.setValue(active.size());
        countSold.setValue(soldOut.size());
        countInactive.setValue(inactive.size());
    }


    public void toggleProductStatus(String productId, boolean isNowActive) {
        isLoading.setValue(true);
        productRepository.updateProductStatus(productId, isNowActive, new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                updateStatusSuceess.setValue(true);
                fetchMyProducts();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void deleteProduct(String productId) {
        isLoading.setValue(true);
        productRepository.deleteProduct(productId, new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                deleteSuccess.setValue(true);
                fetchMyProducts();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

}
