package com.example.florist.views.seller.createshop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.florist.R;
import com.mapbox.search.result.SearchSuggestion;
import com.mapbox.search.result.SearchSuggestion;
import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<SearchSuggestion> suggestions = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SearchSuggestion suggestion);
    }

    public SearchResultAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setSuggestions(List<SearchSuggestion> newSuggestions) {
        this.suggestions = newSuggestions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchSuggestion suggestion = suggestions.get(position);

        // GUNAKAN getName() dari SearchSuggestion
        holder.tvName.setText(suggestion.getName());

        // Ambil alamat jika ada
        if (suggestion.getAddress() != null) {
            holder.tvAddress.setText(suggestion.getAddress().formattedAddress());
        } else {
            holder.tvAddress.setText("");
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(suggestion));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_result_name);
            tvAddress = itemView.findViewById(R.id.tv_result_address);
        }
    }
}