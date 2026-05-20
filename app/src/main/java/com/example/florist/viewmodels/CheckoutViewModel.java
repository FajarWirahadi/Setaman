package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.CartItem;
import com.example.florist.model.CheckoutRepository;
import com.example.florist.model.DeliveryAddress;
import com.example.florist.model.Order;
import com.example.florist.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CheckoutViewModel extends ViewModel {
    private final CheckoutRepository repo = new CheckoutRepository();

    private final MutableLiveData<List<CartItem>> checkoutList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<DeliveryAddress> selectedAddress = new MutableLiveData<>();
    private final MutableLiveData<String> currentBuyerName = new MutableLiveData<>("Hamba Allah");

    private final MutableLiveData<Long> subTotal = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> shippingCost = new MutableLiveData<>(15000L);
    private final MutableLiveData<Long> grandTotal = new MutableLiveData<>(0L);
    private final MutableLiveData<String> midtransToken = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isOrderSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private boolean isDirectBuy = false;

    // --- Getters ---
    public LiveData<List<CartItem>> getCheckoutList() { return checkoutList; }
    public LiveData<DeliveryAddress> getSelectedAddress() { return selectedAddress; }
    public LiveData<Long> getSubTotal() { return subTotal; }
    public LiveData<Long> getShippingCost() { return shippingCost; }
    public LiveData<Long> getGrandTotal() { return grandTotal; }
    public LiveData<Boolean> getIsOrderSuccess() { return isOrderSuccess; }
    public LiveData<String> getMidtransToken() {return midtransToken;}
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    // --- Setters / Actions ---
    public void setDirectBuyItem(CartItem item) {
        isDirectBuy = true;
        List<CartItem> list = new ArrayList<>();
        if (item != null) list.add(item);
        checkoutList.setValue(list);
        calculateTotals(list);
    }

    public void setSelectedAddress(DeliveryAddress address) {
        selectedAddress.setValue(address);
    }

    // --- Load Data dari Repo ---
    public void loadInitialData() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        isLoading.setValue(true);

        // 1. Ambil Nama Pembeli
        repo.getUserProfile(userId, new CheckoutRepository.DataCallback<User>() {
            @Override
            public void onSuccess(User data) { currentBuyerName.setValue(data.getUsername()); }
            @Override
            public void onError(String error) { /* Biarkan default */ }
        });

        repo.getMainAddress(userId, new CheckoutRepository.DataCallback<DeliveryAddress>() {
            @Override
            public void onSuccess(DeliveryAddress data) { selectedAddress.setValue(data); }
            @Override
            public void onError(String error) { errorMessage.setValue(error); }
        });

        if (!isDirectBuy) {
            repo.getCartItems(userId, new CheckoutRepository.DataCallback<List<CartItem>>() {
                @Override
                public void onSuccess(List<CartItem> data) {
                    checkoutList.setValue(data);
                    calculateTotals(data);
                    isLoading.setValue(false);
                }
                @Override
                public void onError(String error) {
                    errorMessage.setValue(error);
                    isLoading.setValue(false);
                }
            });
        } else {
            isLoading.setValue(false);
        }
    }

    private void calculateTotals(List<CartItem> items) {
        long tempSubTotal = 0;
        for (CartItem item : items) {
            int multiplier = 1;
            if ("Mingguan".equals(item.getDurationType())) multiplier = 7;
            else if ("Bulanan".equals(item.getDurationType())) multiplier = 30;
            tempSubTotal += (long) item.getPrice() * item.getQuantity() * item.getDurationValue() * multiplier;
        }
        subTotal.setValue(tempSubTotal);

        long ongkir = shippingCost.getValue() != null ? shippingCost.getValue() : 15000L;
        grandTotal.setValue(tempSubTotal + ongkir);
    }

    public void processOrder(String paymentMethod) {
        String userId = FirebaseAuth.getInstance().getUid();
        List<CartItem> items = checkoutList.getValue();
        DeliveryAddress address = selectedAddress.getValue();

        if (userId == null || items == null || items.isEmpty() || address == null) {
            errorMessage.setValue("Data pesanan tidak lengkap! Pastikan alamat dipilih.");
            return;
        }
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            errorMessage.setValue("Pilih metode pembayaran terlebih dahulu!");
            return;
        }

        isLoading.setValue(true);

        repo.processCheckout(items, userId, address, paymentMethod, isDirectBuy, new CheckoutRepository.ActionCallback() {
            @Override
            public void onSuccess(String orderId) {
                if (paymentMethod.equals("COD")) {
                    isLoading.setValue(false);
                    isOrderSuccess.setValue(true);
                } else {
                    listenForMidTransToken(orderId);
                }
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal Checkout: " + error);
            }
        });
    }

    private void listenForMidTransToken(String orderId) {
        FirebaseFirestore.getInstance().collection("orders").document(orderId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Gagal memantau pembayaran: " + e.getMessage());
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        String token = doc.getString("snapToken");

                        if (token != null && !token.isEmpty()) {
                            isLoading.setValue(false);

                            if (token.equals("ERROR_DARI_SERVER")) {
                                errorMessage.setValue("Server gagal memproses pembayaran ke Midtrans.");
                            } else {
                                midtransToken.setValue(token);
                            }
                        }
                    }
                });
    }
}