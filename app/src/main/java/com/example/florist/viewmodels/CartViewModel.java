package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.CartItem;
import com.example.florist.repository.AuthRepository;
import com.example.florist.repository.CartRepository;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class CartViewModel extends ViewModel {
    private final CartRepository cartRepository;
    private final AuthRepository authRepository;

    private final MutableLiveData<List<CartItem>> cartItems = new MutableLiveData<>();
    private final MutableLiveData<Long> totalPrice = new MutableLiveData<>();
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> cartBadgeCount = new MutableLiveData<>();

    public CartViewModel() {
        cartRepository = new CartRepository();
        authRepository = AuthRepository.getInstance();
    }

    public LiveData<List<CartItem>> getCartItems() {return cartItems;}
    public LiveData<Long> getTotalPrice() {return totalPrice;}
    public LiveData<String> getActionMessage() {return actionMessage;}
    public LiveData<Integer> getCartBadgeCount() {return cartBadgeCount;}

    public void loadCartCount() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            cartRepository.getCartItemCount(user.getUid(), new CartRepository.CountCallback() {
                @Override
                public void onSuccess(int count) {
                    cartBadgeCount.setValue(count);
                }

                @Override
                public void onError(String errorMessage) {
                    cartBadgeCount.setValue(0);
                }
            });
        } else {
            cartBadgeCount.setValue(0);
        }
    }

    public void listenToMyCart() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            cartRepository.listenToCartItems(user.getUid(), new CartRepository.CartListCallback() {
                @Override
                public void onDataChange(List<CartItem> items) {
                    cartItems.setValue(items);
                    calculateTotalPrice(items); // Hitung total setiap kali data berubah!
                }

                @Override
                public void onError(String message) {
                    actionMessage.setValue("Gagal memuat keranjang: " + message);
                }
            });
        }
    }

    // ARSITEKTUR BERSIH: Perhitungan harga dilakukan oleh Otak (ViewModel)
    private void calculateTotalPrice(List<CartItem> list) {
        long grandTotal = 0;
        for (CartItem item : list) {
            int multiplier = 1;
            if ("Mingguan".equals(item.getDurationType())) multiplier = 7;
            else if ("Bulanan".equals(item.getDurationType())) multiplier = 30;

            grandTotal += (long) item.getPrice() * item.getQuantity() * item.getDurationValue() * multiplier;
        }
        totalPrice.setValue(grandTotal);
    }

    // --- AKSI PENGGUNA ---

    public void updateQuantity(CartItem item, int newQty) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null && newQty > 0) {
            cartRepository.updateCartItem(user.getUid(), item.getProductId(), "quantity", newQty, new CartRepository.ActionCallback() {
                @Override public void onSuccess() { loadCartCount(); } // Update badge global
                @Override public void onError(String message) { actionMessage.setValue(message); }
            });
        }
    }

    public void deleteItem(CartItem item) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            cartRepository.deleteCartItem(user.getUid(), item.getProductId(), new CartRepository.ActionCallback() {
                @Override public void onSuccess() {
                    actionMessage.setValue("Bunga dihapus dari keranjang");
                    loadCartCount();
                }
                @Override public void onError(String message) { actionMessage.setValue(message); }
            });
        }
    }

    public void updateDuration(CartItem item, int newValue, String newType) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("durationValue", newValue);
            updates.put("durationType", newType);

            cartRepository.updateCartItemMultiple(user.getUid(), item.getProductId(), updates, new CartRepository.ActionCallback() {
                @Override public void onSuccess() { /* Berhasil, snapshot otomatis update UI */ }
                @Override public void onError(String message) { actionMessage.setValue(message); }
            });
        }
    }
}
