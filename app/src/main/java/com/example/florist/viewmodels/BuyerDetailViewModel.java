package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BuyerDetailViewModel extends ViewModel {
    private final MutableLiveData<Integer> quantity = new MutableLiveData<>(1);
    private final MutableLiveData<Integer> durationValue = new MutableLiveData<>(1);
    private final MutableLiveData<String> durationType = new MutableLiveData<>("Harian");
    private final MutableLiveData<Integer> durationMultiplier = new MutableLiveData<>(1);
    private final MutableLiveData<Long> totalPrice = new MutableLiveData<>(0L);

    private double basePrice = 0;
    private int maxStock = 0;

    public LiveData<Integer> getQuantity() {return quantity;}
    public LiveData<Integer> getDurationValue() {return durationValue;}
    public LiveData<String> getDurationType() {return durationType;}
    public LiveData<Integer> getDurationMultiplier() {return durationMultiplier;}
    public LiveData<Long> getTotalPrice() {return totalPrice;}

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
