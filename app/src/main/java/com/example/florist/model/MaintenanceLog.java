package com.example.florist.model;

import com.google.firebase.Timestamp;

public class MaintenanceLog {
    private String logId;
    private String description;
    private String imageUrl;
    private Timestamp createdAt;

    public MaintenanceLog() {
    }

    public MaintenanceLog(String logID, String description, String imageUrl, Timestamp createdAt) {
        this.logId = logID;
        this.description = description;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogID(String logID) {
        this.logId = logID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
