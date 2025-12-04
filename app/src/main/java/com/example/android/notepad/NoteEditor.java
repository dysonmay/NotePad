/* 注释和头部信息保持不变 */
package com.example.android.notepad;

import static com.example.android.notepad.R.*;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NoteEditor extends AppCompatActivity {
    private static final String TAG = "NoteEditor";

    private static final String[] PROJECTION =
            new String[] {
                    NotePad.Notes._ID,
                    NotePad.Notes.COLUMN_NAME_TITLE,
                    NotePad.Notes.COLUMN_NAME_NOTE,
                    NotePad.Notes.COLUMN_NAME_TAG_ID  // 添加标签ID列
            };

    private static final String ORIGINAL_CONTENT = "origContent";
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private String mOriginalContent;
    private String mOriginalTitle;
    private boolean mContentChanged = false;
    private boolean mTitleChanged = false;
    private long mTagId = 0;
    private String mTagName = "";
    private int mTagColor = 0;
    private Button mTagButton;
    private static final int MENU_EDIT_TITLE=1001;
    private static final int REQUEST_EDIT_TITLE = 1002;

    public static class LinedEditText extends androidx.appcompat.widget.AppCompatEditText {
        private Rect mRect;
        private Paint mPaint;
        private Paint mMarginPaint;
        private Context mContext;

        // 此构造函数由 LayoutInflater 使用
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
            mContext = context;

            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            // 使用动态主题的主色作为线条颜色
            mPaint.setColor(ThemeManager.getThemeColor(context, 0));

            // 设置线条宽度（可以根据需要调整）
            mPaint.setStrokeWidth(2f);

            mMarginPaint = new Paint();
            mMarginPaint.setStyle(Paint.Style.STROKE);
            // 使用灰色或较浅的颜色作为边距线
            mMarginPaint.setColor(Color.GRAY);

            // 设置边距线宽度
            mMarginPaint.setStrokeWidth(1f);

            setLineSpacing(4f, 1f);
        }

        public void updateLineColor() {
            mPaint.setColor(ThemeManager.getThemeColor(mContext, 0));
            invalidate();
        }
        // 辅助方法：从主题获取颜色属性值
        private int getThemeAttrColor(Context context, int attr) {
            TypedValue typedValue = new TypedValue();
            TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[]{attr});
            int color = a.getColor(0, Color.GRAY); // 默认灰色
            a.recycle();
            return color;
        }



        @Override
        protected void onDraw(Canvas canvas) {
            // 更新线条颜色，确保使用当前主题
            mPaint.setColor(ThemeManager.getThemeColor(mContext, 0));

            // 获取 View 中文本的行数。
            int count = getLineCount();
            int height = getHeight();
            int lineHeight = getLineHeight();
            int paddingTop = getPaddingTop();
            int paddingBottom = getPaddingBottom();

            // 获取全局 Rect 和 Paint 对象
            Rect r = mRect;
            Paint paint = mPaint;
            Paint marginPaint = mMarginPaint;

            // 绘制左边距线
            int margin = getPaddingLeft();
            canvas.drawLine(margin, paddingTop, margin, height - paddingBottom, marginPaint);

            /*
             * 为 EditText 中的每一行文本在矩形中绘制一条线
             */
            for (int i = 0; i < count; i++) {
                // 获取当前文本行的基线坐标
                int baseline = getLineBounds(i, r);

                /*
                 * 在背景中从矩形的左侧到右侧绘制一条线，
                 * 在基线下方一个 dip 的垂直位置，使用 "paint" 对象
                 * 获取详细信息。
                 */
                canvas.drawLine(r.left, baseline + 4, r.right, baseline + 4, paint);
            }

            // 通过调用父方法完成
            super.onDraw(canvas);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_editor);

        mText = (EditText) findViewById(R.id.note);
        mTagButton = (Button) findViewById(R.id.tag_button);

        if (mText == null) {
            Log.e(TAG, "EditText 'note' not found in layout!");
            finish();
            return;
        }

        if (mTagButton == null) {
            Log.e(TAG, "Button 'tag_button' not found in layout!");
        }

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_EDIT.equals(action)) {
            mState = STATE_EDIT;
            mUri = intent.getData();
            Log.d(TAG, "Editing note with URI: " + mUri);
        } else if (Intent.ACTION_INSERT.equals(action) || Intent.ACTION_PASTE.equals(action)) {
            mState = STATE_INSERT;

            ContentValues initialValues = new ContentValues();
            initialValues.put(NotePad.Notes.COLUMN_NAME_TAG_ID, 0);
            initialValues.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

            mUri = getContentResolver().insert(intent.getData(), initialValues);

            if (mUri == null) {
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
                Toast.makeText(this, "创建笔记失败", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Log.d(TAG, "Inserted new note with URI: " + mUri);
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
        } else {
            Log.e(TAG, "Unknown action: " + action + ", exiting");
            Toast.makeText(this, "未知操作", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mContentChanged = true;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (mTagButton != null) {
            mTagButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTagSelectionDialog();
                }
            });
        }

        // 查询笔记数据，包括标签ID
        mCursor = getContentResolver().query(
                mUri,
                PROJECTION,
                null,
                null,
                null
        );

        if (mCursor != null && mCursor.moveToFirst()) {
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
            int colTagIdIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TAG_ID);

            if (colNoteIndex != -1) {
                String note = mCursor.getString(colNoteIndex);
                mOriginalContent = note;
                mText.setText(note);
                mText.setSelection(mText.getText().length());
            }

            if (colTitleIndex != -1) {
                mOriginalTitle = mCursor.getString(colTitleIndex);
            }

            // 加载标签信息
            if (colTagIdIndex != -1) {
                mTagId = mCursor.getLong(colTagIdIndex);
                Log.d(TAG, "Loaded tag id: " + mTagId);

                // 立即更新标签按钮
                if (mTagId > 0) {
                    loadTagInfo(mTagId);
                } else {
                    updateTagButton();
                }
            }
        } else {
            Log.e(TAG, "Failed to query note data");
            Toast.makeText(this, "加载笔记失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadTagInfo(long tagId) {
        try {
            Uri tagUri = ContentUris.withAppendedId(NotePad.Notes.TAG_ID_URI_BASE, tagId);
            Cursor tagCursor = getContentResolver().query(
                    tagUri,
                    new String[]{
                            NotePad.Notes.COLUMN_NAME_TAG_NAME,
                            NotePad.Notes.COLUMN_NAME_TAG_COLOR
                    },
                    null, null, null
            );

            if (tagCursor != null && tagCursor.moveToFirst()) {
                mTagName = tagCursor.getString(0);
                mTagColor = tagCursor.getInt(1);
                tagCursor.close();
                updateTagButton();
            } else {
                // 如果标签不存在，重置为无标签
                mTagId = 0;
                updateTagButton();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading tag info", e);
            mTagId = 0;
            updateTagButton();
        }
    }

    private void updateTagButton() {
        if (mTagButton != null) {
            if (mTagId > 0) {
                mTagButton.setText(mTagName);
                mTagButton.setBackgroundColor(mTagColor);
                mTagButton.setTextColor(getContrastColor(mTagColor));
            } else {
                mTagButton.setText(getString(R.string.tag_none));
                mTagButton.setBackgroundColor(0xFFE0E0E0);
                mTagButton.setTextColor(Color.BLACK);
            }
        }
    }

    private int getContrastColor(int color) {
        double luminance = 0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color);
        return luminance > 186 ? Color.BLACK : Color.WHITE;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mText instanceof LinedEditText) {
            ((LinedEditText) mText).updateLineColor();
        }

        if (mCursor != null) {
            mCursor.requery();
            mCursor.moveToFirst();

            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
            int colTagIdIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TAG_ID);

            String note = mCursor.getString(colNoteIndex);
            mOriginalContent = note;
            mOriginalTitle = mCursor.getString(colTitleIndex);

            mText.setTextKeepState(note);
            mContentChanged = false;
            mTitleChanged = false;

            // 重新加载标签信息
            long currentTagId = mCursor.getLong(colTagIdIndex);
            if (currentTagId != mTagId) {
                mTagId = currentTagId;
                if (mTagId > 0) {
                    loadTagInfo(mTagId);
                } else {
                    updateTagButton();
                }
            }

            if (mState == STATE_EDIT) {
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null) {
            String text = mText.getText().toString();
            int length = text.length();

            if (isFinishing() && (length == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();
            } else if (mState == STATE_EDIT) {
                if (!text.equals(mOriginalContent)) {
                    updateNote(text, null);
                } else if (mTagId != getCurrentTagIdFromCursor()) {
                    // 如果只是标签改变了，也更新笔记
                    updateNote(text, null);
                }
            } else if (mState == STATE_INSERT) {
                updateNote(text, text);
                mState = STATE_EDIT;
            }
        }
    }

    private long getCurrentTagIdFromCursor() {
        if (mCursor != null && !mCursor.isClosed()) {
            int colTagIdIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TAG_ID);
            if (colTagIdIndex != -1) {
                return mCursor.getLong(colTagIdIndex);
            }
        }
        return 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        if (mState == STATE_EDIT) {
            // 为编辑标题菜单项设置一个唯一的ID
            MenuItem editTitleItem = menu.add(Menu.NONE, MENU_EDIT_TITLE, Menu.NONE, R.string.resolve_title);
            editTitleItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        MenuItem tagItem = menu.add(Menu.NONE, Menu.FIRST + 100, Menu.NONE, R.string.menu_tag);
        tagItem.setIcon(android.R.drawable.ic_menu_agenda);
        tagItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
        String savedNote = mCursor.getString(colNoteIndex);
        String currentNote = mText.getText().toString();
        if (savedNote.equals(currentNote)) {
            menu.findItem(R.id.menu_revert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_revert).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Menu.FIRST + 100) {
            showTagSelectionDialog();
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.menu_save) {
            String text = mText.getText().toString();
            updateNote(text, null);
            finish();
        } else if (id == R.id.menu_delete) {
            deleteNote();
            finish();
        } else if (id == R.id.menu_revert) {
            cancelNote();
        } else if (id == MENU_EDIT_TITLE) {
            // 添加编辑标题的处理逻辑
            showTitleEditor();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 添加一个新的方法来显示标题编辑器
    private void showTitleEditor() {
        if (mUri != null) {
            TitleEditor editor = TitleEditor.newInstance(mUri);
            // 设置监听器，当标题修改完成后更新UI
            editor.setTitleEditListener(new TitleEditor.OnTitleEditListener() {
                @Override
                public void onTitleEdited(String newTitle) {
                    // 更新本地变量
                    mOriginalTitle = newTitle;
                    mTitleChanged = true;

                    // 更新笔记标题显示
                    if (mState == STATE_EDIT) {
                        Resources res = getResources();
                        String text = String.format(res.getString(R.string.title_edit), newTitle);
                        setTitle(text);
                    }

                    // 通知用户
                    Toast.makeText(NoteEditor.this, "标题已更新", Toast.LENGTH_SHORT).show();

                    // 标记内容已更改
                    mContentChanged = true;
                }
            });
            editor.show(getSupportFragmentManager(), "title_editor");
        }
    }

    private void showTagSelectionDialog() {
        TagSelectionDialog dialog = TagSelectionDialog.newInstance(mTagId);
        dialog.setOnTagSelectedListener(new TagSelectionDialog.OnTagSelectedListener() {
            @Override
            public void onTagSelected(long tagId, String tagName, int tagColor) {
                mTagId = tagId;
                mTagName = tagName;
                mTagColor = tagColor;
                updateTagButton();
                updateNoteWithTag();
            }

            @Override
            public void onTagCreated(long newTagId, String tagName, int tagColor) {
                mTagId = newTagId;
                mTagName = tagName;
                mTagColor = tagColor;
                updateTagButton();
                updateNoteWithTag();
            }

            @Override
            public void onFilterByTag(long tagId) {
                // 在编辑界面不需要实现筛选功能
            }
        });
        dialog.show(getSupportFragmentManager(), "tag_selection");
    }

    private final void performPaste() {

        // 获取剪贴板管理器的句柄
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        // 获取内容解析器实例
        ContentResolver cr = getContentResolver();

        // 从剪贴板获取剪贴板数据
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {

            String text=null;
            String title=null;

            // 从剪贴板数据中获取第一个项目
            ClipData.Item item = clip.getItemAt(0);

            // 尝试将项目的内容作为指向笔记的 URI 获取
            Uri uri = item.getUri();

            // 测试项目实际上是一个 URI，并且该 URI
            // 是一个内容 URI，指向其 MIME 类型与 Note pad 提供程序支持的 MIME 类型相同的提供程序。
            if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {

                // 剪贴板持有对具有笔记 MIME 类型数据的引用。这会复制它。
                Cursor orig = cr.query(
                        uri,            // 内容提供程序的 URI
                        PROJECTION,     // 获取投影中引用的列
                        null,           // 没有选择变量
                        null,           // 没有选择变量，因此不需要条件
                        null            // 使用默认排序顺序
                );

                // 如果 Cursor 不为 null，并且它包含至少一条记录
                //（moveToFirst() 返回 true），则从中获取笔记数据。
                if (orig != null) {
                    if (orig.moveToFirst()) {
                        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                        int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                        text = orig.getString(colNoteIndex);
                        title = orig.getString(colTitleIndex);
                    }

                    // 关闭游标。
                    orig.close();
                }
            }

            // 如果剪贴板的内容不是对笔记的引用，则
            // 这将其转换为文本。
            if (text == null) {
                text = item.coerceToText(this).toString();
            }

            // 使用检索到的标题和文本更新当前笔记。
            updateNote(text, title);
        }
    }


    private final void updateNote(String text, String title) {
        boolean contentChanged = !text.equals(mOriginalContent);
        boolean titleChanged = (title != null) && !title.equals(mOriginalTitle);

        if (mState == STATE_EDIT && !contentChanged && !titleChanged && mTagId == getCurrentTagIdFromCursor()) {
            return;
        }

        ContentValues values = new ContentValues();

        // 如果内容、标题或标签有变化，更新时间
        if (contentChanged || titleChanged || mState == STATE_INSERT || mTagId != getCurrentTagIdFromCursor()) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        }

        // 确保标签ID被保存
        values.put(NotePad.Notes.COLUMN_NAME_TAG_ID, mTagId);

        if (mState == STATE_INSERT) {
            if (title == null) {
                int length = text.length();
                title = text.substring(0, Math.min(30, length));
                if (length > 30) {
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        title = title.substring(0, lastSpace);
                    }
                }
            }
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        } else if (title != null) {
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        }

        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

        int rowsUpdated = getContentResolver().update(
                mUri,
                values,
                null,
                null
        );

        Log.d(TAG, "updateNote: rowsUpdated = " + rowsUpdated + ", tagId = " + mTagId);

        if (rowsUpdated > 0) {
            // 更新原始内容
            mOriginalContent = text;
            if (title != null) {
                mOriginalTitle = title;
            }
            mContentChanged = false;
            mTitleChanged = false;
        }
    }

    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);
                getContentResolver().update(mUri, values, null, null);
            } else if (mState == STATE_INSERT) {
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }

    private void updateNoteWithTag() {
        if (mUri != null) {
            ContentValues values = new ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_TAG_ID, mTagId);
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

            int rowsUpdated = getContentResolver().update(
                    mUri,
                    values,
                    null,
                    null
            );

            Log.d(TAG, "updateNoteWithTag: rowsUpdated = " + rowsUpdated + ", tagId = " + mTagId);

            if (rowsUpdated > 0) {
                // 通知数据已更改
                getContentResolver().notifyChange(mUri, null);
                Toast.makeText(this, "标签已更新", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "标签更新失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}