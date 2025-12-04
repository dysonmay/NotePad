package com.example.android.notepad;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class ColorPickerDialog extends DialogFragment {

    public interface ColorPickerListener {
        void onColorSelected(int color);
    }

    private ColorPickerListener listener;
    private int initialColor;

    public static ColorPickerDialog newInstance(int initialColor) {
        ColorPickerDialog dialog = new ColorPickerDialog();
        Bundle args = new Bundle();
        args.putInt("initialColor", initialColor);
        dialog.setArguments(args);
        return dialog;
    }

    public void setColorPickerListener(ColorPickerListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            initialColor = getArguments().getInt("initialColor", Color.BLUE);
        }

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext());

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_color_picker, null);

        GridView gridView = view.findViewById(R.id.color_grid);

        // 颜色数组
        final int[] colors = {
                0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
                0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
                0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
                0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722,
                0xFF795548, 0xFF9E9E9E, 0xFF607D8B, 0xFF000000
        };

        ColorGridAdapter adapter = new ColorGridAdapter(requireContext(), colors);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            if (listener != null) {
                listener.onColorSelected(colors[position]);
            }
            dismiss();
        });

        builder.setView(view)
                .setTitle("选择颜色")
                .setNegativeButton("取消", null);

        return builder.create();
    }
}