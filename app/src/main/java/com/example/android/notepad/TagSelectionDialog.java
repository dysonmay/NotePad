package com.example.android.notepad;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class TagSelectionDialog extends DialogFragment {

    public interface OnTagSelectedListener {
        void onTagSelected(long tagId, String tagName, int tagColor);
        void onTagCreated(long newTagId, String tagName, int tagColor);
        void onFilterByTag(long tagId);
    }

    private OnTagSelectedListener listener;
    private long selectedTagId;
    private long noteTagId;
    private List<Tag> tags = new ArrayList<>();
    private TagAdapter tagAdapter;
    private GridView tagGridView;
    private MaterialButton btnManageTags;
    private View tagColorPreview;
    private EditText etTagName;
    private LinearLayout colorPalette;
    private Button btnPickColor;
    private Button btnCreateTag;

    private static final int[] COLOR_PALETTE = {
            0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
            0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
            0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
            0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722,
            0xFF795548, 0xFF9E9E9E, 0xFF607D8B, 0xFF000000
    };

    private int selectedColor = 0xFF2196F3;

    public static TagSelectionDialog newInstance(long currentTagId) {
        TagSelectionDialog dialog = new TagSelectionDialog();
        Bundle args = new Bundle();
        args.putLong("currentTagId", currentTagId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnTagSelectedListener(OnTagSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            noteTagId = getArguments().getLong("currentTagId", 0);
            selectedTagId = noteTagId;
        }

        loadTags();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.tag_selection_dialog, null);

        initViews(dialogView);
        setupListeners();

        builder.setView(dialogView)
                .setTitle(getString(R.string.tag_select))
                .setPositiveButton(getString(R.string.button_ok), (dialog, which) -> {
                    if (selectedTagId != noteTagId && listener != null) {
                        Tag selectedTag = getTagById(selectedTagId);
                        if (selectedTag != null) {
                            listener.onTagSelected(selectedTagId, selectedTag.getName(), selectedTag.getColor());
                        } else if (selectedTagId == 0) {
                            listener.onTagSelected(0, getString(R.string.tag_none), 0xFFE0E0E0);
                        }
                    }
                })
                .setNegativeButton(getString(android.R.string.cancel), null);

        return builder.create();
    }

    private void initViews(View view) {
        tagGridView = view.findViewById(R.id.tag_grid);
        btnManageTags = view.findViewById(R.id.btn_manage_tags);
        tagColorPreview = view.findViewById(R.id.tag_color_preview);
        etTagName = view.findViewById(R.id.et_tag_name);
        colorPalette = view.findViewById(R.id.color_palette);
        btnPickColor = view.findViewById(R.id.btn_pick_color);
        btnCreateTag = view.findViewById(R.id.btn_create_tag);

        tagAdapter = new TagAdapter(requireContext(), tags, selectedTagId);
        tagGridView.setAdapter(tagAdapter);

        tagColorPreview.setBackgroundColor(selectedColor);
        setupColorPalette();

        // 设置标签网格点击事件
        tagGridView.setOnItemClickListener((parent, view1, position, id) -> {
            Tag tag = tags.get(position);
            selectedTagId = tag.getId();
            tagAdapter.setSelectedTagId(selectedTagId);
            tagAdapter.notifyDataSetChanged();
        });
    }

    private void setupListeners() {
        btnManageTags.setOnClickListener(v -> showManageTagsDialog());

        btnPickColor.setOnClickListener(v -> showColorPickerDialog());

        btnCreateTag.setOnClickListener(v -> createNewTag());
    }

    private void loadTags() {
        tags.clear();

        Cursor cursor = requireContext().getContentResolver().query(
                NotePad.Notes.TAGS_CONTENT_URI,
                new String[]{"_id",
                        NotePad.Notes.COLUMN_NAME_TAG_NAME,
                        NotePad.Notes.COLUMN_NAME_TAG_COLOR,
                        "created_date"},
                null,
                null,
                "created_date ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Tag tag = new Tag();
                tag.setId(cursor.getLong(0));
                tag.setName(cursor.getString(1));
                tag.setColor(cursor.getInt(2));
                tag.setCreatedDate(cursor.getLong(3));
                tags.add(tag);
            }
            cursor.close();
        }

        Tag noTag = new Tag();
        noTag.setId(0);
        noTag.setName(getString(R.string.tag_none));
        noTag.setColor(0xFFE0E0E0);
        tags.add(0, noTag);
    }

    private void setupColorPalette() {
        if (colorPalette == null) return;

        colorPalette.removeAllViews();

        int size = (int) (getResources().getDisplayMetrics().density * 32);
        int margin = (int) (getResources().getDisplayMetrics().density * 4);

        for (int color : COLOR_PALETTE) {
            View colorView = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(color);
            colorView.setTag(color);

            colorView.setOnClickListener(v -> {
                selectedColor = (int) v.getTag();
                if (tagColorPreview != null) {
                    tagColorPreview.setBackgroundColor(selectedColor);
                }
            });

            colorPalette.addView(colorView);
        }
    }

    private void showColorPickerDialog() {
        ColorPickerDialog dialog = ColorPickerDialog.newInstance(selectedColor);
        dialog.setColorPickerListener(color -> {
            selectedColor = color;
            if (tagColorPreview != null) {
                tagColorPreview.setBackgroundColor(color);
            }
        });
        dialog.show(getParentFragmentManager(), "color_picker");
    }

    private void createNewTag() {
        String tagName = etTagName.getText().toString().trim();
        if (TextUtils.isEmpty(tagName)) {
            Toast.makeText(requireContext(), "请输入标签名称", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, tagName);
        values.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, selectedColor);
        values.put("created_date", System.currentTimeMillis());

        Uri newTagUri = requireContext().getContentResolver().insert(
                NotePad.Notes.TAGS_CONTENT_URI,
                values
        );

        if (newTagUri != null) {
            long newTagId = ContentUris.parseId(newTagUri);

            Tag newTag = new Tag(newTagId, tagName, selectedColor);
            tags.add(newTag);
            tagAdapter.notifyDataSetChanged();

            // 自动选择新创建的标签
            selectedTagId = newTagId;
            tagAdapter.setSelectedTagId(selectedTagId);
            tagAdapter.notifyDataSetChanged();

            etTagName.setText("");

            Toast.makeText(requireContext(), R.string.tag_created_success, Toast.LENGTH_SHORT).show();

            if (listener != null) {
                listener.onTagCreated(newTagId, tagName, selectedColor);
            }
        }
    }

    private void showManageTagsDialog() {
        ManageTagsDialog dialog = ManageTagsDialog.newInstance();
        dialog.show(getParentFragmentManager(), "manage_tags");
    }

    private Tag getTagById(long tagId) {
        for (Tag tag : tags) {
            if (tag.getId() == tagId) {
                return tag;
            }
        }
        return null;
    }

    private class TagAdapter extends ArrayAdapter<Tag> {
        private long selectedTagId;
        private Context context;

        TagAdapter(Context context, List<Tag> tags, long selectedTagId) {
            super(context, R.layout.item_tag, tags);
            this.context = context;
            this.selectedTagId = selectedTagId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_tag, parent, false);
            }

            Tag tag = getItem(position);
            if (tag == null) return convertView;

            MaterialCardView cardView = convertView.findViewById(R.id.tag_card);
            TextView tvTagName = convertView.findViewById(R.id.tv_tag_name);
            View colorIndicator = convertView.findViewById(R.id.color_indicator);

            tvTagName.setText(tag.getName());
            colorIndicator.setBackgroundColor(tag.getColor());

            boolean isSelected = (tag.getId() == selectedTagId);
            cardView.setStrokeWidth(isSelected ?
                    (int) (getResources().getDisplayMetrics().density * 2) : 0);
            cardView.setStrokeColor(tag.getColor());

            return convertView;
        }

        public void setSelectedTagId(long tagId) {
            this.selectedTagId = tagId;
            notifyDataSetChanged();
        }
    }
}