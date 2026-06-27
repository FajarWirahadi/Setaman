package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.CartItem;
import com.example.florist.model.DeliveryAddress;
import com.example.florist.model.User;
import com.example.florist.repository.AuthRepository;
import com.example.florist.repository.CheckoutRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CheckoutViewModel extends ViewModel {
    private final CheckoutRepository repo = new CheckoutRepository();
    private ListenerRegistration tokenListener;
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

    public void loadInitialData() {
        String userId = AuthRepository.getInstance().getCurrentUserId();
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
            long duration = item.getDurationValue() > 0 ? item.getDurationValue() : 1;
            long itemBasePrice = (long) item.getPrice();

            String type = item.getDurationType() != null ? item.getDurationType() : "";

            if (type.equalsIgnoreCase("Mingguan") || type.equalsIgnoreCase("Minggu")) {
                itemBasePrice *= 7;
            } else if (type.equalsIgnoreCase("Bulanan") || type.equalsIgnoreCase("Bulan")) {
                itemBasePrice *= 30;
            }

            tempSubTotal += itemBasePrice * item.getQuantity() * duration;
        }
        subTotal.setValue(tempSubTotal);

        long ongkir = shippingCost.getValue() != null ? shippingCost.getValue() : 15000L;
        grandTotal.setValue(tempSubTotal + ongkir);
    }

    public void processOrder(String paymentMethod) {
        String userId = AuthRepository.getInstance().getCurrentUserId();
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        List<CartItem> items = checkoutList.getValue();
        DeliveryAddress address = selectedAddress.getValue();

        String buyerName = currentBuyerName.getValue() != null ? currentBuyerName.getValue() : "Pelanggan";

        if (userId == null || items == null || items.isEmpty() || address == null) {
            errorMessage.setValue("Data pesanan tidak lengkap! Pastikan alamat dipilih.");
            return;
        }
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            errorMessage.setValue("Pilih metode pembayaran terlebih dahulu!");
            return;
        }

        long ongkir = shippingCost.getValue() != null ? shippingCost.getValue() : 15000L;

        isLoading.setValue(true);

        repo.processCheckout(items, userId, buyerName, address, paymentMethod, ongkir, isDirectBuy, new CheckoutRepository.ActionCallback() {
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
        if (tokenListener != null) tokenListener.remove();

        tokenListener = repo.listenForPaymentToken(orderId, new CheckoutRepository.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                isLoading.setValue(false);

                if ("ERROR_DARI_SERVER".equals(token)) {
                    errorMessage.setValue("Server gagal memproses pembayaran ke Midtrans.");
                } else {
                    midtransToken.setValue(token);
                }

                if (tokenListener != null) tokenListener.remove();
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue("Gagal memantau pembayaran: " + error);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (tokenListener != null) tokenListener.remove();
    }
}