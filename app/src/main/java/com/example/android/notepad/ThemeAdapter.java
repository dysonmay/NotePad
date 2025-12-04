package com.example.android.notepad;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {

    private String[] themeNames;
    private int[][] themeColors;
    private int selectedIndex;
    private OnThemeSelectedListener listener;

    public interface OnThemeSelectedListener {
        void onThemeSelected(int position);
    }

    public ThemeAdapter(Context context, int selectedIndex) {
        this.themeNames = context.getResources().getStringArray(R.array.theme_names);
        this.themeColors = ThemeManager.getThemeColors();
        this.selectedIndex = selectedIndex;
    }

    public void setOnThemeSelectedListener(OnThemeSelectedListener listener) {
        this.listener = listener;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.theme_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvThemeName.setText(themeNames[position]);
        holder.colorIndicator.setBackgroundColor(themeColors[position][0]);

        holder.ivSelected.setVisibility(
                position == selectedIndex ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onThemeSelected(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return themeNames.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View colorIndicator;
        TextView tvThemeName;
        ImageView ivSelected;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.color_indicator);
            tvThemeName = itemView.findViewById(R.id.tv_theme_name);
            ivSelected = itemView.findViewById(R.id.iv_selected);
        }
    }
}