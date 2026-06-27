package com.example.florist.model;

public class MaintenanceTaskUIModel {
    public Rental rental;
    public String displayOrderId;
    public String displayDuration;
    public String displayNextDate;
    public String taskStatusText;
    public int statusColorCode; // Hex color
    public boolean showCompleteButton;
    public String buttonText;
    public int buttonColorCode;

    // Tambahan untuk komplain
    public String activeComplaintId;
    public boolean isComplaintVisit;
}