package com.example.florist.viewmodels;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.mapbox.geojson.Point;

public class MapsViewModel extends ViewModel {

    private final MutableLiveData<String> addressName = new MutableLiveData<>();

    private final MutableLiveData<String> addressDetail = new MutableLiveData<>();

    private final MutableLiveData<Point> selectedPoint = new MutableLiveData<Point>();

    public void setLocationDetail(Point point, String name, String detail){
        selectedPoint.setValue(point);
        addressName.setValue(name);
        addressDetail.setValue(detail);
    }

    public LiveData<String> getAddressName() {return addressName;}
    public LiveData<String> getAddressDetail() {return addressDetail;}
    public LiveData<Point> getSelectedPoint() {return selectedPoint;}
}
