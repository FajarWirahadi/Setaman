package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.ChatRoom;
import com.example.florist.repository.InboxRepository;

import java.util.List;

public class InboxViewModel extends ViewModel {

    private final InboxRepository repository;
    private final MutableLiveData<List<ChatRoom>> inboxList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public InboxViewModel() {
        repository = new InboxRepository();
    }

    public LiveData<List<ChatRoom>> getInboxList() { return inboxList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void fetchInbox(String userId) {
        isLoading.setValue(true);
        repository.listenToInbox(userId, new InboxRepository.InboxCallback() {
            @Override
            public void onInboxUpdated(List<ChatRoom> chatRooms) {
                isLoading.setValue(false);
                inboxList.setValue(chatRooms);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void loadMyInbox() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            fetchInbox(currentUser.getUid());
        } else {
            errorMessage.setValue("Sesi berakhir, silakan login kembali.");
        }
    }
}