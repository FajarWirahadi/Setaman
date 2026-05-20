package com.example.florist.viewmodels;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.florist.model.MediaRepository;
import com.zhihu.matisse.Matisse;

import java.util.List;

public class UploadViewModel extends AndroidViewModel {

    private final MediaRepository repository;

    private final MutableLiveData<List<Uri>> selectedMedia = new MutableLiveData<List<Uri>>();
    public LiveData<List<Uri>> getSelectedMedia() {
        return selectedMedia;
    }

    private  final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    public  LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public UploadViewModel(@NonNull Application application) {
        super(application);
        repository = new MediaRepository();
    }

    public void  handleResult(Intent data) {
        if (data != null) {
            List<Uri> rawUris = Matisse.obtainResult(data);
            if (rawUris != null && !rawUris.isEmpty()) {
                List<Uri> processed = repository.processSelectedMedia(rawUris);

                selectedMedia.setValue(processed);
                statusMessage.setValue("Berhasil Memilih " + processed.size() + " file.");
            }
        }
    }
}
