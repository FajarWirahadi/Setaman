package com.example.florist.views.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.adapter.MessageAdapter;
import com.example.florist.databinding.ActivityChatRoomBinding;
import com.example.florist.viewmodels.AuthViewModel;
import com.example.florist.viewmodels.ChatViewModel;
import com.example.florist.views.seller.RentalDetailActivity;

public class ChatRoomActivity extends AppCompatActivity {

    private ActivityChatRoomBinding binding;
    private ChatViewModel chatViewModel;
    private AuthViewModel authViewModel;
    private MessageAdapter messageAdapter;

    private String currentUserId;
    private String currentUserName;
    private String targetUserId;
    private String targetUserName;
    private String targetUserImage;

    private String currentRoomId;

    private String draftMessageText;
    private String draftImageUrl, draftRefId, draftRefType, draftRentalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        targetUserId = getIntent().getStringExtra("EXTRA_TARGET_ID");
        targetUserName = getIntent().getStringExtra("EXTRA_TARGET_NAME");
        targetUserImage = getIntent().getStringExtra("EXTRA_TARGET_IMAGE");

        draftMessageText = getIntent().getStringExtra("EXTRA_DRAFT_MESSAGE");
        draftImageUrl = getIntent().getStringExtra("EXTRA_DRAFT_IMAGE");

        draftRefId = getIntent().getStringExtra("EXTRA_DRAFT_REF_ID");
        draftRefType = getIntent().getStringExtra("EXTRA_DRAFT_REF_TYPE");
        draftRentalId = getIntent().getStringExtra("EXTRA_DRAFT_RENTAL_ID");

        if (targetUserId == null) {
            Toast.makeText(this, "Data target tidak valid.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = authViewModel.getCurrentUserId();

        setupUI();
        setupObservers();

        chatViewModel.startChat(targetUserId, targetUserName, targetUserImage);
    }

    private void setupUI() {
        binding.tvChatUserName.setText("Memuat...");

        chatViewModel.loadTargetName(targetUserId, targetUserName);
        Glide.with(this)
                .load(targetUserImage)
                .placeholder(R.drawable.building)
                .centerCrop()
                .into(binding.imgChatUser);

        binding.btnBack.setOnClickListener(v -> onBackPressed());

        messageAdapter = new MessageAdapter(currentUserId);
        messageAdapter.setQuoteClickListener((refId, refType, rentalId) -> {
            Intent intent = new Intent(ChatRoomActivity.this, RentalDetailActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            intent.putExtra("SCROLL_TO_REF_ID", refId);
            intent.putExtra("SCROLL_TO_REF_TYPE", refType);
            intent.putExtra("RENTAL_ID", rentalId);

            startActivity(intent);
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(messageAdapter);

        if (draftMessageText != null || draftImageUrl != null) {
             binding.layoutQuotePreview.setVisibility(View.VISIBLE);

            if (draftMessageText != null) {
                 binding.tvQuotePreview.setText(draftMessageText);

//                binding.etMessageInput.setText(draftMessageText);
//                binding.etMessageInput.requestFocus();
//                binding.etMessageInput.setSelection(draftMessageText.length());
            }

            if (draftImageUrl != null && !draftImageUrl.isEmpty()) {
                 binding.imgQuotePreview.setVisibility(View.VISIBLE);
                 Glide.with(this).load(draftImageUrl).into(binding.imgQuotePreview);
            }

             binding.btnCloseQuote.setOnClickListener(v -> {
                 binding.layoutQuotePreview.setVisibility(View.GONE);
                 draftMessageText = null;
                 draftImageUrl = null;
             });
        }

        binding.btnSendMessage.setOnClickListener(v -> {
            String text = binding.etMessageInput.getText().toString().trim();

            if (text.isEmpty()) {
                Toast.makeText(this, "Pesan tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentRoomId != null) {
                chatViewModel.sendMessage(currentRoomId, currentUserId, text, draftImageUrl, draftRefId, draftRefType, draftRentalId, draftMessageText);

                binding.etMessageInput.setText("");
                draftMessageText = null;
                draftImageUrl = null;
                draftRefType = null;
                draftRentalId = null;
                binding.layoutQuotePreview.setVisibility(View.GONE);

            } else {
                Toast.makeText(this, "Sedang menyiapkan ruang obrolan, tunggu sebentar...", Toast.LENGTH_SHORT).show();

                chatViewModel.startChat(targetUserId, targetUserName, targetUserImage);
            }
        });
    }

    private void setupObservers() {
        chatViewModel.getCurrentRoom().observe(this, room -> {
            if (room != null) {
                currentRoomId = room.getRoomId();
            }
        });

        chatViewModel.getDynamicTargetName().observe(this, name -> {
            if (name != null) {
                binding.tvChatUserName.setText(name);
            }
        });

        chatViewModel.getMessagesList().observe(this, messages -> {
            if (messages != null) {
                messageAdapter.updateMessages(messages);

                if (!messages.isEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });

        chatViewModel.getCurrentRoom().observe(this, room -> {
            if (room != null) {
                currentRoomId = room.getRoomId();
                chatViewModel.markMessagesAsRead(currentRoomId);
            }
        });

        chatViewModel.getErrorMessage().observe(this, error -> {
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