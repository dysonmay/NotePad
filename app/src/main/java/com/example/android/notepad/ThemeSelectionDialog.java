package com.example.android.notepad;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

public class ThemeSelectionDialog extends DialogFragment {

    private ThemeAdapter adapter;
    private int selectedIndex;
    private OnThemeChangeListener listener;

    public interface OnThemeChangeListener {
        void onThemeChanged(int themeIndex);
    }

    public void setOnThemeChangeListener(OnThemeChangeListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getContext();
        selectedIndex = ThemeManager.getCurrentThemeIndex(context);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = View.inflate(context, R.layout.theme_selection_dialog, null);

        RecyclerView recyclerView = view.findViewById(R.id.rv_themes);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        adapter = new ThemeAdapter(context, selectedIndex);
        adapter.setOnThemeSelectedListener(position -> {
            selectedIndex = position;
            adapter.setSelectedIndex(position);
        });

        recyclerView.setAdapter(adapter);

        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        MaterialButton btnOk = view.findViewById(R.id.btn_ok);

        btnCancel.setOnClickListener(v -> dismiss());
        btnOk.setOnClickListener(v -> {
            ThemeManager.setThemeIndex(context, selectedIndex);
            if (listener != null) {
                listener.onThemeChanged(selectedIndex);
            }
            dismiss();
        });

        builder.setView(view);
        return builder.create();
    }
}