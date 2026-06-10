package com.example.florist.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.ChatMessage;
import com.example.florist.model.ChatRoom;
import com.example.florist.repository.ChatRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository repository;

    private final MutableLiveData<ChatRoom> currentRoom = new MutableLiveData<>();
    private final MutableLiveData<List<ChatMessage>> messagesList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ChatViewModel() {
        repository = new ChatRepository();
    }

    public LiveData<ChatRoom> getCurrentRoom() { return currentRoom; }
    public LiveData<List<ChatMessage>> getMessagesList() { return messagesList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void startChat(String targetUserId, String targetUserName, String targetUserImage) {
        FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            String currentUserName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Pengguna";

            initializeRoom(currentUserId, targetUserId, currentUserName, targetUserName, targetUserImage);
        } else {
            errorMessage.setValue("Sesi habis, silakan login kembali.");
        }
    }

    public void initializeRoom(String buyerId, String sellerId, String buyerName, String sellerName, String sellerImageUrl) {
        isLoading.setValue(true);
        repository.createOrGetChatRoom(buyerId, sellerId, buyerName, sellerName, sellerImageUrl, new ChatRepository.RoomCallback() {
            @Override
            public void onSuccess(ChatRoom room) {
                isLoading.setValue(false);
                currentRoom.setValue(room);
                listenForMessages(room.getRoomId());
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void sendMessage(String roomId, String senderId, String text, String imageUrl, String refId, String refType, String rentalId, String refDesc) {

        boolean hasText = text != null && !text.trim().isEmpty();
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();

        if (!hasText && !hasImage) return;

        ChatMessage newMessage = new ChatMessage(senderId, hasText ? text.trim() : "", Timestamp.now());

        if (hasImage) newMessage.setImageUrl(imageUrl);

        if (refId != null && !refId.isEmpty()) {
            newMessage.setReferenceId(refId);
            newMessage.setReferenceType(refType);
            newMessage.setRentalId(rentalId);
            newMessage.setReferenceDesc(refDesc);
        }

        repository.sendMessage(roomId, newMessage, new ChatRepository.ActionCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(String message) {
                errorMessage.setValue(message);
            }
        });
    }

    private void listenForMessages(String roomId) {
        repository.listenForMessages(roomId, new ChatRepository.MessagesCallback() {
            @Override
            public void onMessagesUpdated(List<ChatMessage> messages) {
                messagesList.setValue(messages);
            }

            @Override
            public void onError(String message) {
                errorMessage.setValue("Gagal memuat obrolan: " + message);
            }
        });
    }

    public void markMessagesAsRead(String roomId) {
        FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && roomId != null) {
            repository.resetUnreadCount(roomId, currentUser.getUid());
        }
    }
}