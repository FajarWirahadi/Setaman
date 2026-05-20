package com.example.florist.viewmodels;

import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.model.CartItem;
import com.example.florist.model.CartRepository;
import com.example.florist.model.Product;
import com.example.florist.model.Shop;
import com.example.florist.model.ShopRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class BuyerDetailViewModel extends ViewModel {

    private final CartRepository cartRepository;
    private final ShopRepository shopRepository;

    private final MutableLiveData<Shop> shopData = new MutableLiveData<Shop>();
    private final MutableLiveData<Boolean> addToCartSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> quantity = new MutableLiveData<>(1);
    private final MutableLiveData<Integer> durationValue = new MutableLiveData<>(1);
    private final MutableLiveData<String> durationType = new MutableLiveData<>("Harian");
    private final MutableLiveData<Integer> durationMultiplier = new MutableLiveData<>(1);
    private final MutableLiveData<Long> totalPrice = new MutableLiveData<>(0L);

    private double basePrice = 0;
    private int maxStock = 0;

    public BuyerDetailViewModel() {
        cartRepository = new CartRepository();
        shopRepository = new ShopRepository();
    }

    public LiveData<Integer> getQuantity() {return quantity;}
    public LiveData<Integer> getDurationValue() {return durationValue;}
    public LiveData<String> getDurationType() {return durationType;}
    public LiveData<Integer> getDurationMultiplier() {return durationMultiplier;}
    public LiveData<Long> getTotalPrice() {return totalPrice;}
    public LiveData<Shop> getShopData() {return shopData;}
    public LiveData<Boolean> getAddToCartSuccess() {return addToCartSuccess;}
    public LiveData<String> getErrorMessage() {return errorMessage;}

    public void fetchShopData(String ownerId) {
        shopRepository.getShopById(ownerId, new ShopRepository.ShopDataCallback() {
            @Override
            public void onDataLoaded(Shop shop) {
                shopData.setValue(shop);
            }

            @Override
            public void onError(String message) {
                errorMessage.setValue(message);
            }
        });
    }

    public CartItem createCartItem(Product product, int qty, String durType, int durValue, String shopName) {
        String cartImageUrl = product.getImageUrl();
        if (product.getGallery() != null && !product.getGallery().isEmpty()){
            cartImageUrl = product.getGallery().get(0);
        }

        return new CartItem(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                cartImageUrl,
                product.getOwnerId(),
                shopName,
                qty,
                durType,
                durValue,
                new Date()
        );
    }

    public void addToCart(Product product, int qty, String durType, int durValue, String shopName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            errorMessage.setValue("Silahkan login terlebih dahulu!");
            return;
        }

        CartItem newItem = createCartItem(product, qty, durType, durValue, shopName);
        cartRepository.addToCart(user.getUid(), newItem, new CartRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                addToCartSuccess.setValue(true); // Beri tahu Activity untuk memutar animasi sukses!
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }
    public void setProductData(double basePrice, int maxStock) {
        this.basePrice = basePrice;
        this.maxStock = maxStock;
        calculateTotal();
    }

    public void incrementQuantity() {
        if (quantity.getValue() != null && quantity.getValue() < maxStock) {
            quantity.setValue(quantity.getValue() + 1);
            calculateTotal();
        }
    }

    public void decrementQuantity() {
        if (quantity.getValue() != null && quantity.getValue() > 1) {
            quantity.setValue(quantity.getValue() - 1);
            calculateTotal();
        }
    }

    public void incrementDuration() {
        if (durationValue.getValue() != null) {
            durationValue.setValue(durationValue.getValue() + 1);
            calculateTotal();
        }
    }

    public void decrementDuration() {
        if (durationValue.getValue() != null) {
            durationValue.setValue(durationValue.getValue() - 1);
            calculateTotal();
        }
    }

    public void setDurationType(String type, int multiplier) {
        durationType.setValue(type);
        durationMultiplier.setValue(multiplier);
        calculateTotal();
    }

    private void calculateTotal() {
        if (quantity.getValue() == null || durationValue.getValue() == null || durationMultiplier.getValue() == null) return;

        long total = (long) basePrice * quantity.getValue() * durationValue.getValue() * durationMultiplier.getValue();
        totalPrice.setValue(total);
    }
}
