package com.example.florist.viewmodels;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ProductFormViewModel extends ViewModel {

    //
    private final MutableLiveData<Pair<String, String>> durationData = new MutableLiveData<>();

    private final MutableLiveData<String> scheduleData = new MutableLiveData<>();

    public void setDuration(String amount, String unit) {
        durationData.setValue(new Pair<>(amount, unit));
    }

    public LiveData<Pair<String, String>> getDurationData() {
        return durationData;
    }

    public void setSchedule(String schedule) {
        scheduleData.setValue(schedule);
    }

    public LiveData<String> getScheduleData() {
        return scheduleData;
    }
}
