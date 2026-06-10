package com.example.florist.views.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.florist.adapter.ChatRoomAdapter;
import com.example.florist.databinding.ActivityInboxBinding;
import com.example.florist.viewmodels.AuthViewModel;
import com.example.florist.viewmodels.InboxViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class InboxActivity extends AppCompatActivity {

    private ActivityInboxBinding binding;
    private InboxViewModel inboxViewModel;
    private AuthViewModel authViewModel;
    private ChatRoomAdapter chatRoomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInboxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        inboxViewModel = new ViewModelProvider(this).get(InboxViewModel.class);

        setupUI();
        setupObservers();

        inboxViewModel.loadMyInbox();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        String currentUserId = FirebaseAuth.getInstance().getUid();
        chatRoomAdapter = new ChatRoomAdapter(this, currentUserId);

        binding.rvChatRooms.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChatRooms.setAdapter(chatRoomAdapter);
    }

    private void setupObservers() {
        inboxViewModel.getInboxList().observe(this, chatRooms -> {
            if (chatRooms != null && !chatRooms.isEmpty()) {
                binding.layoutEmptyChat.setVisibility(View.GONE);
                binding.rvChatRooms.setVisibility(View.VISIBLE);
                chatRoomAdapter.setRoomList(chatRooms);
            } else {
                binding.layoutEmptyChat.setVisibility(View.VISIBLE);
                binding.rvChatRooms.setVisibility(View.GONE);
            }
        });

        inboxViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}