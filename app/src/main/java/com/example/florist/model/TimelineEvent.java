package com.example.florist.model;

import com.google.firebase.Timestamp;

public class TimelineEvent {
    public static final int TYPE_ROUTINE = 1;
    public static final int TYPE_COMPLAINT = 2;
    public static final int TYPE_RESOLUTION = 3;

    private int eventType;
    private String eventId; // WAJIB ADA UNTUK REF_ID CHAT
    private Timestamp timestamp;
    private String title;
    private String description;
    private String imageUrl;

    public TimelineEvent(int eventType, String eventId, Timestamp timestamp, String title, String description, String imageUrl) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public int getEventType() { return eventType; }
    public String getEventId() { return eventId; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
}