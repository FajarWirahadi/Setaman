package com.example.florist.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MediaViewModel extends ViewModel {
    private final MutableLiveData<List<Object>> _selectedMedia = new MutableLiveData<>();

    public LiveData<List<Object>> getSelectedMedia() {
        return _selectedMedia;
    }

    public void updateMediaList(List<?> newUris) {
        List<Object> currentList = _selectedMedia.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        // Gabungkan list lama + list baru
        // Tambahkan validasi contains agar tidak duplikat jika user memilih ulang foto yang sama
        for (Object uri : newUris) {
            if (!currentList.contains(uri)) {
                currentList.add(uri);
            }
        }

        // Update LiveData agar Observer di Activity merespons
        _selectedMedia.setValue(currentList);
    }

    public void removeMedia(Object uri) {
        List<Object> currentList = _selectedMedia.getValue();
        if (currentList != null) {
            // Buat list baru agar LiveData mendeteksi perubahan
            List<Object> updatedList = new ArrayList<>(currentList);

            updatedList.remove(uri); // Hapus item

            _selectedMedia.setValue(updatedList); // Update LiveData
        }
    }

    public Uri getFirstMediaUri() {
        List<Object> list = _selectedMedia.getValue();
        if (list != null && !list.isEmpty()) {
            return (Uri) list.get(0);
        }
        return null;
    }

    public List<Uri> getNewImagesOnly() {
        List<Uri> newImages = new ArrayList<>();
        List<Object> current = _selectedMedia.getValue();
        if (current != null) {
            for (Object item : current) {
                if (item instanceof Uri) {
                    newImages.add((Uri) item);
                }
            }
        }
        return newImages;
    }

    // Cek apakah gambar lama masih ada (tidak dihapus user)
    public String getRemainingOldImageUrl() {
        List<Object> current = _selectedMedia.getValue();
        if (current != null) {
            for (Object item : current) {
                if (item instanceof String) {
                    return (String) item; // Kembalikan URL lama pertama yg ditemukan
                }
            }
        }
        return null; // Artinya gambar lama sudah dihapus user
    }
}
