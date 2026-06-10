package com.example.florist.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.Rental;
import com.example.florist.repository.MaintenanceRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MaintenanceViewModel extends ViewModel {
    private final MaintenanceRepository repository = new MaintenanceRepository();

    // State Holders
    private final MutableLiveData<List<Rental>> allRentals = new MutableLiveData<>();
    private final MutableLiveData<List<Rental>> filteredRentals = new MutableLiveData<>();
    private final MutableLiveData<Date> selectedDate = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionSuccessMessage = new MutableLiveData<>();

    public LiveData<List<Rental>> getFilteredRentals() { return filteredRentals; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getActionSuccessMessage() { return actionSuccessMessage; }

    public void setSelectedDate(Date date) {
        selectedDate.setValue(date);
        applyDateFilter();
    }

    public void fetchSellerRentals(String status) {
        isLoading.setValue(true);
        repository.getSellerMaintenanceSchedule(status, new MaintenanceRepository.RentalListCallback() {
            @Override
            public void onSuccess(List<Rental> rentals) {
                isLoading.setValue(false);
                allRentals.setValue(rentals);
                applyDateFilter();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    private void applyDateFilter() {
        List<Rental> rentals = allRentals.getValue();
        Date targetDate = selectedDate.getValue();

        if (rentals == null || targetDate == null) return;

        List<Rental> filteredList = new ArrayList<>();

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0); todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0);

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(targetDate);
        targetCal.set(Calendar.HOUR_OF_DAY, 0); targetCal.set(Calendar.MINUTE, 0); targetCal.set(Calendar.SECOND, 0); targetCal.set(Calendar.MILLISECOND, 0);

        boolean isTargetToday = todayCal.getTimeInMillis() == targetCal.getTimeInMillis();

        for (Rental rental : rentals) {
            if ("PROSES PERBAIKAN".equalsIgnoreCase(rental.getStatus()) || "Komplain".equalsIgnoreCase(rental.getStatus())) {
                filteredList.add(rental);
                continue;
            }
            if (isMaintenanceDay(rental, targetDate)) {
                filteredList.add(rental);
                continue;
            }

            if (isTargetToday && isOverdue(rental, todayCal.getTime())) {
                filteredList.add(rental);
            }
        }
        filteredRentals.setValue(filteredList);
    }

    private boolean isMaintenanceDay(Rental rental, Date targetDate) {
        if (rental.getStartDate() == null) return false;

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(targetDate);
        targetCal.set(Calendar.HOUR_OF_DAY, 0); targetCal.set(Calendar.MINUTE, 0); targetCal.set(Calendar.SECOND, 0); targetCal.set(Calendar.MILLISECOND, 0);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(rental.getStartDate().toDate());
        startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);

        if (targetCal.before(startCal)) return false;

        if (rental.getEndDate() != null) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(rental.getEndDate().toDate());
            endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59);
            if (targetCal.after(endCal)) return false;
        }

        long diffMillis = targetCal.getTimeInMillis() - startCal.getTimeInMillis();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);

        return (diffDays % 3) == 0;
    }

    private boolean isOverdue(Rental rental, Date today) {
        if (rental.getStartDate() == null) return false;

        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(today);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(rental.getStartDate().toDate());
        startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);

        if (todayCal.before(startCal)) return false;

        long diffMillis = todayCal.getTimeInMillis() - startCal.getTimeInMillis();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);
        long lastScheduledDay = diffDays - (diffDays % 3);

        if (lastScheduledDay <= 0) return false;

        Calendar lastScheduledCal = (Calendar) startCal.clone();
        lastScheduledCal.add(Calendar.DAY_OF_YEAR, (int) lastScheduledDay);

        Calendar actualLastMaintCal = Calendar.getInstance();
        if (rental.getLastMaintenanceDate() != null) {
            actualLastMaintCal.setTime(rental.getLastMaintenanceDate().toDate());
        } else {
            actualLastMaintCal.setTime(rental.getStartDate().toDate());
        }
        actualLastMaintCal.set(Calendar.HOUR_OF_DAY, 0); actualLastMaintCal.set(Calendar.MINUTE, 0); actualLastMaintCal.set(Calendar.SECOND, 0); actualLastMaintCal.set(Calendar.MILLISECOND, 0);

        return actualLastMaintCal.before(lastScheduledCal);
    }

    public void AddMaintenance(Rental rental, Uri imageUri, String description) {
        isLoading.setValue(true);
        repository.submitMaintenanceLog(rental, imageUri, description, new MaintenanceRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                actionSuccessMessage.setValue("Laporan perawatan berhasil dikirim!");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }
}