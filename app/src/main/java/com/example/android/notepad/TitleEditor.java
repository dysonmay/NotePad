package com.example.android.notepad;

import android.app.Dialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 此Fragment允许用户编辑笔记的标题。它显示一个对话框，
 * 其中包含一个Material Design风格的EditText。
 */
public class TitleEditor extends DialogFragment {

    private static final String ARG_URI = "note_uri";

    // 创建一个投影，返回笔记ID和笔记内容。
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
    };

    // 在提供程序返回的Cursor中，标题列的位置。
    private static final int COLUMN_INDEX_TITLE = 1;

    // 一个Cursor对象，将包含向提供程序查询笔记的结果。
    private Cursor mCursor;

    // 一个EditText对象，用于保存编辑后的标题。
    private TextInputEditText mEditText;
    private TextInputLayout mTextInputLayout;

    // 一个URI对象，指向正在编辑标题的笔记。
    private Uri mUri;
    private String mOriginalTitle;

    /**
     * 创建TitleEditor的新实例。
     * @param uri 要编辑的笔记的URI
     * @return TitleEditor实例
     */
    public static TitleEditor newInstance(Uri uri) {
        TitleEditor fragment = new TitleEditor();
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_Dialog);

        // 从arguments中获取URI
        if (getArguments() != null) {
            mUri = getArguments().getParcelable(ARG_URI);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.title_editor, container, false);

        setupViews(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 设置对话框标题
        if (getDialog() != null) {
            getDialog().setTitle(getString(R.string.resolve_title));
        }

        // 加载当前标题
        loadCurrentTitle();
    }

    @Override
    public void onResume() {
        super.onResume();

        // 设置对话框尺寸
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void setupViews(View view) {
        mEditText = view.findViewById(R.id.title);
        mTextInputLayout = view.findViewById(R.id.text_input_layout);

        MaterialButton buttonCancel = view.findViewById(R.id.button_cancel);
        MaterialButton buttonOk = view.findViewById(R.id.button_ok);

        buttonCancel.setOnClickListener(v -> dismiss());
        buttonOk.setOnClickListener(v -> saveAndDismiss());

        // 设置文本变化监听器
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateOkButtonState(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 加载当前笔记的标题。
     */
    private void loadCurrentTitle() {
        if (mUri != null && getContext() != null) {
            mCursor = getContext().getContentResolver().query(
                    mUri,
                    PROJECTION,
                    null,
                    null,
                    null
            );

            if (mCursor != null && mCursor.moveToFirst()) {
                String currentTitle = mCursor.getString(COLUMN_INDEX_TITLE);
                mEditText.setText(currentTitle);
                mOriginalTitle = currentTitle;
                mEditText.setSelection(currentTitle.length());
                updateOkButtonState(currentTitle.trim());
            } else {
                // 如果游标为空或查询失败，设置默认标题
                mOriginalTitle = "";
                updateOkButtonState("");
            }
        }
    }

    /**
     * 更新确定按钮的状态。
     * @param currentText 当前输入框中的文本
     */
    private void updateOkButtonState(String currentText) {
        View view = getView();
        if (view != null) {
            MaterialButton buttonOk = view.findViewById(R.id.button_ok);
            if (buttonOk != null) {
                boolean hasChanged = !currentText.equals(mOriginalTitle != null ? mOriginalTitle.trim() : "");
                boolean isValid = !currentText.isEmpty();
                buttonOk.setEnabled(hasChanged && isValid);

                // 如果输入为空，显示错误提示
                if (mTextInputLayout != null) {
                    if (currentText.isEmpty()) {
                        mTextInputLayout.setError(getString(R.string.nothing_to_save));
                    } else {
                        mTextInputLayout.setError(null);
                    }
                }
            }
        }
    }

    /**
     * 保存标题并关闭对话框。
     */
    private void saveAndDismiss() {
        String currentTitle = mEditText.getText().toString().trim();

        if (!currentTitle.isEmpty() && !currentTitle.equals(mOriginalTitle)) {
            if (getContext() != null && mUri != null) {
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_TITLE, currentTitle);
                values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

                int rowsUpdated = getContext().getContentResolver().update(
                        mUri,
                        values,
                        null,
                        null
                );

                if (rowsUpdated > 0) {
                    // 通知监听器标题已修改
                    if (mListener != null) {
                        mListener.onTitleEdited(currentTitle);
                    }
                }
            }
        }
        dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }

    public interface OnTitleEditListener {
        void onTitleEdited(String newTitle);
    }

    // 添加一个成员变量
    private OnTitleEditListener mListener;

    // 添加设置监听器的方法
    public void setTitleEditListener(OnTitleEditListener listener) {
        mListener = listener;
    }
}