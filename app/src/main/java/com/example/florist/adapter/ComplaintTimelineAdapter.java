package com.example.florist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.florist.R;
import com.example.florist.model.Complaint;
import com.example.florist.model.Rental;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ComplaintTimelineAdapter extends RecyclerView.Adapter<ComplaintTimelineAdapter.ViewHolder> {
    private final List<Complaint> complaintList = new ArrayList<>();
    private final List<Rental> rentalList = new ArrayList<>();
    private String storeName = "Penjual";

    private String buyerName = "Pelanggan";
    private boolean isSellerMode = false;

    public interface OnComplaintQuoteListener {
        void onQuoteClicked(Complaint complaint);
    }

    private OnComplaintQuoteListener quoteListener;

    public void setQuoteListener(OnComplaintQuoteListener listener) {
        this.quoteListener = listener;
    }

    public void setBuyerName(String buyerName) {
        if (buyerName != null && !buyerName.isEmpty()) {
            this.buyerName = buyerName;
            notifyDataSetChanged();
        }
    }

    public void setSellerMode(boolean isSellerMode) {
        this.isSellerMode = isSellerMode;
        notifyDataSetChanged();
    }

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));

    public void setComplaints(List<Complaint> newList) {
        complaintList.clear();
        complaintList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_complaint_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Complaint complaint = complaintList.get(position);

        if (complaint.getCreatedAt() != null) {
            holder.tvDate.setText(sdf.format(complaint.getCreatedAt().toDate()));
        }
        holder.tvReason.setText("Alasan: " + complaint.getReason());

        if (isSellerMode) {
            String buyerAction = "<b>" + buyerName + "</b>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                holder.tvActionTitle.setText(android.text.Html.fromHtml(buyerAction, android.text.Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.tvActionTitle.setText(android.text.Html.fromHtml(buyerAction));
            }
        } else {
            holder.tvActionTitle.setText("Anda");
        }

        holder.tvDesc.setText(complaint.getDescription());
        String evidendeImageUrl = complaintList.get(position).getEvidenceImageUrl();

        holder.imgEvidence.setOnClickListener(v -> {
            if (evidendeImageUrl != null && !evidendeImageUrl.isEmpty()) {
                showZoomableImageDialog(holder.itemView.getContext(), evidendeImageUrl);
            }
        });

        String sellerImageUrl = complaintList.get(position).getSellerImageUrl();

        holder.imgSellerEvidence.setOnClickListener(v -> {
            if (sellerImageUrl != null && !sellerImageUrl.isEmpty()) {
                showZoomableImageDialog(holder.itemView.getContext(), sellerImageUrl);
            }
        });

        if (complaint.getEvidenceImageUrl() != null && !complaint.getEvidenceImageUrl().isEmpty()) {
            holder.imgEvidence.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(complaint.getEvidenceImageUrl())
                    .into(holder.imgEvidence);
        } else {
            holder.imgEvidence.setVisibility(View.GONE);
        }

        if (("Resolved".equalsIgnoreCase(complaint.getStatus()) ||
                "Responded".equalsIgnoreCase(complaint.getStatus()) ||
                "Menunggu Konfirmasi".equalsIgnoreCase(complaint.getStatus())) &&
                complaint.getSellerResponseText() != null) {

            holder.cardResponse.setVisibility(View.VISIBLE);
            String message = "<b>" + storeName + "</b> menanggapi <b>Komplain anda</b>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                holder.tvResponseTitle.setText(android.text.Html.fromHtml(message, android.text.Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.tvResponseTitle.setText(android.text.Html.fromHtml(message));
            }
            holder.tvResponseText.setText(complaint.getSellerResponseText());

            if (complaint.getSellerImageUrl() != null && !complaint.getSellerImageUrl().isEmpty()) {
                holder.imgSellerEvidence.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(complaint.getSellerImageUrl())
                        .into(holder.imgSellerEvidence);
            } else {
                holder.imgSellerEvidence.setVisibility(View.GONE);
            }

        } else {
            holder.viewConnector.setVisibility(View.GONE);
            holder.cardResponse.setVisibility(View.GONE);
        }

        if (complaint.getEvidenceImageUrl() != null && !complaint.getEvidenceImageUrl().isEmpty()) {
            holder.imgEvidence.setVisibility(View.VISIBLE);
            holder.btnQuoteComplaint.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(complaint.getEvidenceImageUrl())
                    .into(holder.imgEvidence);
        } else {
            holder.imgEvidence.setVisibility(View.GONE);
            holder.btnQuoteComplaint.setVisibility(View.GONE);
        }

        if (complaint.getSellerImageUrl() != null && !complaint.getSellerImageUrl().isEmpty()) {
            holder.imgSellerEvidence.setVisibility(View.VISIBLE);
            holder.btnQuoteComplaint1.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(complaint.getSellerImageUrl())
                    .into(holder.imgSellerEvidence);
        } else {
            holder.imgSellerEvidence.setVisibility(View.GONE);
            holder.btnQuoteComplaint1.setVisibility(View.GONE);
        }

        holder.btnQuoteComplaint.setOnClickListener(v -> {
            if (quoteListener != null) {
                quoteListener.onQuoteClicked(complaint);
            }
        });

        holder.btnQuoteComplaint1.setOnClickListener(v -> {
            if (quoteListener != null) {
                quoteListener.onQuoteClicked(complaint);
            }
        });
    }
//        holder.viewLineBottom.setVisibility(position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);
//        holder.viewLineTop.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
//        holder.viewLineBottom.setVisibility(position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);

    @Override
    public int getItemCount() {
        return complaintList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvReason, tvDesc, tvResponseText, tvResponseTitle, tvActionTitle, tvRole;
        ImageView imgEvidence, imgSellerEvidence;
        View cardResponse, viewConnector;
        View viewLineTop, viewLineBottom;
        MaterialCardView btnQuoteComplaint, btnQuoteComplaint1;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvComplaintDate);
            tvReason = itemView.findViewById(R.id.tvComplaintReason);
            tvDesc = itemView.findViewById(R.id.tvComplaintDesc);
            imgEvidence = itemView.findViewById(R.id.imgComplaintEvidence);
            imgSellerEvidence = itemView.findViewById(R.id.imgSellerEvidence);
            cardResponse = itemView.findViewById(R.id.cardSellerResponse);
            tvResponseText = itemView.findViewById(R.id.tvSellerResponseText);
            tvResponseTitle = itemView.findViewById(R.id.tvResponseTitle);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvActionTitle = itemView.findViewById(R.id.tvActionTitle);
            viewConnector = itemView.findViewById(R.id.viewConnector);
            viewLineTop = itemView.findViewById(R.id.viewLineTop);
            viewLineBottom = itemView.findViewById(R.id.viewLineBottom);
            btnQuoteComplaint = itemView.findViewById(R.id.btnQuoteComplaint);
            btnQuoteComplaint1 = itemView.findViewById(R.id.btnQuoteComplaint2);
        }
    }

    public void setStoreName(String storeName) {
        if (storeName != null && !storeName.isEmpty()) {
            this.storeName = storeName;
            notifyDataSetChanged();
        }
    }

    private void showZoomableImageDialog(android.content.Context context, String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_zoom_layout);

        com.github.chrisbanes.photoview.PhotoView photoView = dialog.findViewById(R.id.photoView);
        android.widget.ImageButton btnClose = dialog.findViewById(R.id.btnCloseZoom);

        com.bumptech.glide.Glide.with(context)
                .load(imageUrl)
                .into(photoView);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}