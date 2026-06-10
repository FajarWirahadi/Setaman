package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Rental;
import com.example.florist.repository.RentalRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class RentalViewModel extends ViewModel {
    private final RentalRepository repository = new RentalRepository();
    private ListenerRegistration registration;

    private final MutableLiveData<List<Rental>> rentals = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionSuccessMessage = new MutableLiveData<>();


    public LiveData<List<Rental>> getRentals() { return rentals; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getActionSuccessMessage() { return actionSuccessMessage; }

    public void loadMyRentals() {
        isLoading.setValue(true);

        registration = repository.listenToBuyerRentals(new RentalRepository.RentalListCallback() {
            @Override
            public void onSuccess(List<Rental> data) {
                isLoading.setValue(false);
                rentals.setValue(data);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (registration != null) {
            registration.remove();
        }
    }
}