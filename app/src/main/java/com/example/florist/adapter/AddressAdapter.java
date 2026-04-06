package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.florist.R;
import com.example.florist.databinding.ItemDeliveryAddressBinding;
import com.example.florist.model.DeliveryAddress;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    private Context context;
    private List<DeliveryAddress> addressList;
    private int selectedPosition = -1;
    private AddressClickListener listener;

    public interface AddressClickListener {
        void onAddressClick(DeliveryAddress address, int position);
        void onEditClick(DeliveryAddress address, int position);
    }

    public AddressAdapter(Context context, List<DeliveryAddress> addressList, AddressClickListener listener) {
        this.context = context;
        this.addressList = addressList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeliveryAddressBinding binding = ItemDeliveryAddressBinding.inflate(LayoutInflater.from(context), parent, false);
        return new AddressViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        DeliveryAddress address = addressList.get(position);

        holder.binding.tvAddressLabel.setText(address.getLabel());
        holder.binding.tvReceiverInfo.setText(address.getReceiverName() + " (" + address.getPhoneNumber() + ")");
        holder.binding.tvFullAddress.setText(address.getFullAddress());

        // Atur kemunculan Badge "Main Address"
        if (address.isMainAddress()) {
            holder.binding.tvMainBadge.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvMainBadge.setVisibility(View.GONE);
        }

        // Atur kemunculan Centang Hijau
        if (selectedPosition == position) {
            holder.binding.imgCheckmark.setVisibility(View.VISIBLE);
            // Opsional: Beri warna border/background beda jika terpilih
            holder.binding.cardAddress.setBackgroundResource(com.example.florist.R.drawable.rounded_green_border_button);
        } else {
            holder.binding.imgCheckmark.setVisibility(View.GONE);
            holder.binding.cardAddress.setBackgroundResource(com.example.florist.R.drawable.rounded_white_button);
        }

        if (address.getLatitude() != 0.0 && address.getLongitude() != 0.0) {
            // Jika ada koordinatnya
            holder.binding.tvPinpointStatus.setText("Titik peta tersimpan");
            holder.binding.tvPinpointStatus.setTextColor(context.getResources().getColor(R.color.olive_500));
        } else {
            // Jika pembeli dulu hanya mengetik alamat manual tanpa buka peta
            holder.binding.tvPinpointStatus.setText("Belum ada titik peta");
            holder.binding.tvPinpointStatus.setTextColor(context.getResources().getColor(R.color.gray_400));
        }

        // Event Klik Kartu
        holder.binding.getRoot().setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            listener.onAddressClick(address, selectedPosition);
        });

        // Event Klik Tombol Edit
        holder.binding.btnEditAddress.setOnClickListener(v -> {
            listener.onEditClick(address, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        ItemDeliveryAddressBinding binding;

        public AddressViewHolder(@NonNull ItemDeliveryAddressBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}