package com.example.florist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.florist.R;
import com.example.florist.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final Context context;
    private final List<Category> categoryList;
    private int selectedPosition = -1;
    private OnCategorySelectedListener listener;

    public interface OnCategorySelectedListener{
        void onCategorySelectedListener(Category category);
    }

    public CategoryAdapter(Context context, List<Category> categoryList, OnCategorySelectedListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
    }


    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvCategory.setText(category.getName());

        holder.itemView.setOnClickListener(v -> {
            if(listener != null) {
                listener.onCategorySelectedListener(category);
            }
        });


    }


    @Override
    public int getItemCount() {
       return categoryList.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder  {
        TextView tvCategory;
        RelativeLayout rlCategory;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            rlCategory = itemView.findViewById(R.id.rlCategory);
        }

    }

}

