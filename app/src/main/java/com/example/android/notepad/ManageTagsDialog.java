package com.example.android.notepad;

import android.app.Dialog;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class ManageTagsDialog extends DialogFragment {

    private List<Tag> tags = new ArrayList<>();
    private ArrayAdapter<Tag> adapter;
    private ListView tagListView;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_manage_tags, null);

        tagListView = view.findViewById(R.id.tag_list);
        Button btnAddTag = view.findViewById(R.id.btn_add_tag);

        loadTags();

        adapter = new ArrayAdapter<Tag>(requireContext(),
                android.R.layout.simple_list_item_1, tags) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_manage_tag, parent, false);
                }

                Tag tag = getItem(position);
                TextView tvTagName = convertView.findViewById(R.id.tv_tag_name);
                TextView tvTagColor = convertView.findViewById(R.id.tv_tag_color);
                View colorView = convertView.findViewById(R.id.color_preview);
                Button btnDelete = convertView.findViewById(R.id.btn_delete_tag);
                Button btnEdit = convertView.findViewById(R.id.btn_edit_tag);

                tvTagName.setText(tag.getName());
                colorView.setBackgroundColor(tag.getColor());

                // 显示颜色值
                String colorHex = String.format("#%06X", (0xFFFFFF & tag.getColor()));
                tvTagColor.setText(colorHex);

                // 删除按钮
                btnDelete.setOnClickListener(v -> {
                    if (tag.getId() > 0) { // 不删除"无标签"
                        deleteTag(tag);
                    }
                });

                // 编辑按钮
                btnEdit.setOnClickListener(v -> {
                    showEditTagDialog(tag);
                });

                return convertView;
            }
        };

        tagListView.setAdapter(adapter);

        btnAddTag.setOnClickListener(v -> {
            showAddTagDialog();
        });

        builder.setView(view)
                .setTitle("管理标签")
                .setNegativeButton("关闭", null);

        return builder.create();
    }

    private void loadTags() {
        tags.clear();

        // 加载所有标签，跳过"无标签"
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
    }

    private void showAddTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_tag, null);

        EditText etTagName = view.findViewById(R.id.et_tag_name);
        Button btnPickColor = view.findViewById(R.id.btn_pick_color);
        View colorPreview = view.findViewById(R.id.color_preview);

        int[] selectedColor = {0xFF2196F3}; // 默认蓝色
        colorPreview.setBackgroundColor(selectedColor[0]);

        btnPickColor.setOnClickListener(v -> {
            showColorPickerDialog(colorPreview, selectedColor);
        });

        builder.setView(view)
                .setTitle("添加标签")
                .setPositiveButton("保存", (dialog, which) -> {
                    String tagName = etTagName.getText().toString().trim();
                    if (!tagName.isEmpty()) {
                        addTag(tagName, selectedColor[0]);
                    } else {
                        Toast.makeText(requireContext(), "请输入标签名称", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showEditTagDialog(Tag tag) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_tag, null);

        EditText etTagName = view.findViewById(R.id.et_tag_name);
        Button btnPickColor = view.findViewById(R.id.btn_pick_color);
        View colorPreview = view.findViewById(R.id.color_preview);

        etTagName.setText(tag.getName());
        colorPreview.setBackgroundColor(tag.getColor());

        int[] selectedColor = {tag.getColor()};

        btnPickColor.setOnClickListener(v -> {
            showColorPickerDialog(colorPreview, selectedColor);
        });

        builder.setView(view)
                .setTitle("编辑标签")
                .setPositiveButton("保存", (dialog, which) -> {
                    String newName = etTagName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateTag(tag, newName, selectedColor[0]);
                    } else {
                        Toast.makeText(requireContext(), "请输入标签名称", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showColorPickerDialog(View colorPreview, int[] selectedColor) {
        ColorPickerDialog dialog = ColorPickerDialog.newInstance(selectedColor[0]);
        dialog.setColorPickerListener(color -> {
            selectedColor[0] = color;
            colorPreview.setBackgroundColor(color);
        });
        dialog.show(getParentFragmentManager(), "color_picker");
    }

    private void addTag(String name, int color) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, name);
        values.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, color);
        values.put("created_date", System.currentTimeMillis());

        Uri newUri = requireContext().getContentResolver().insert(
                NotePad.Notes.TAGS_CONTENT_URI,
                values
        );

        if (newUri != null) {
            loadTags();
            adapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), "标签添加成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "标签添加失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTag(Tag tag, String newName, int newColor) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, newName);
        values.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, newColor);

        Uri tagUri = ContentUris.withAppendedId(NotePad.Notes.TAG_ID_URI_BASE, tag.getId());
        int count = requireContext().getContentResolver().update(
                tagUri,
                values,
                null,
                null
        );

        if (count > 0) {
            loadTags();
            adapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), "标签更新成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "标签更新失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteTag(Tag tag) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("删除标签")
                .setMessage("确定要删除标签 \"" + tag.getName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    Uri tagUri = ContentUris.withAppendedId(NotePad.Notes.TAG_ID_URI_BASE, tag.getId());
                    int deleted = requireContext().getContentResolver().delete(
                            tagUri,
                            null,
                            null
                    );

                    if (deleted > 0) {
                        // 更新所有使用该标签的笔记，将其标签设为0（无标签）
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(NotePad.Notes.COLUMN_NAME_TAG_ID, 0);
                        requireContext().getContentResolver().update(
                                NotePad.Notes.CONTENT_URI,
                                values,
                                NotePad.Notes.COLUMN_NAME_TAG_ID + " = ?",
                                new String[]{String.valueOf(tag.getId())}
                        );

                        loadTags();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(requireContext(), "标签已删除", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "标签删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    public static ManageTagsDialog newInstance() {
        return new ManageTagsDialog();
    }
}