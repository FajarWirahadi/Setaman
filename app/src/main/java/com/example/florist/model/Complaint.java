package com.example.florist.model;

import com.google.firebase.Timestamp;

public class Complaint {
    private String complaintId;
    private String reason;
    private String description;
    private String evidenceImageUrl;
    private String sellerImageUrl;
    private String status;
    private Timestamp createdAt;
    private String sellerResponseText = null;
    private String buyerResponseText = null;
    private Timestamp resolvedAt = null;

    public Complaint(){

    }

    public Complaint(String complaintId, String reason, String description, String evidenceImageUrl,
                           String status, Timestamp createdAt){
        this.complaintId = complaintId;
        this.reason = reason;
        this.description = description;
        this.evidenceImageUrl = evidenceImageUrl;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(String privateId) {
        this.complaintId = privateId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidenceImageUrl() {
        return evidenceImageUrl;
    }

    public void setEvidenceImageUrl(String evidenceImageUrl) {
        this.evidenceImageUrl = evidenceImageUrl;
    }

    public String getSellerImageUrl() {
        return sellerImageUrl;
    }

    public void setSellerImageUrl(String sellerImageUrl) {
        this.sellerImageUrl = sellerImageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getSellerResponseText() {
        return sellerResponseText;
    }

    public void setSellerResponseText(String sellerResponseText) {
        this.sellerResponseText = sellerResponseText;
    }

    public String getBuyerResponseText() {
        return buyerResponseText;
    }

    public void setBuyerResponseText(String buyerResponseText) {
        this.buyerResponseText = buyerResponseText;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
