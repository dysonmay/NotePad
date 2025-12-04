package com.example.android.notepad;

import com.example.android.notepad.NotePad;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

        /**
 * 显示笔记列表。如果启动Intent中提供了{@link Uri}，则显示该Uri对应的笔记，
 * 否则默认显示{@link NotePadProvider}的内容。
 */
public class NotesList extends AppCompatActivity {

            // 用于日志记录和调试
            private static final String TAG = "NotesList";

            /**
             * 游标适配器所需的列
             */
            private static final String[] PROJECTION = new String[]{
                    NotePad.Notes._ID, // 0
                    NotePad.Notes.COLUMN_NAME_TITLE, // 1
                    NotePad.Notes.COLUMN_NAME_NOTE, // 2
                    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 3
                    NotePad.Notes.COLUMN_NAME_TAG_ID // 4 标签ID列
            };

            /**
             * 标题列的索引
             */
            private long mCurrentFilterTagId = -1; // -1 表示不过滤
            private static final int COLUMN_INDEX_TITLE = 1;
            private static final int COLUMN_INDEX_NOTE = 2;
            private static final int COLUMN_INDEX_MODIFICATION_DATE = 3;
            private static final int COLUMN_INDEX_TAG_ID = 4;

            private Button mTagFilterButton;
            private Map<Long, Tag> mTagCache = new HashMap<>();

            private Handler searchHandler = new Handler();
            private Runnable searchRunnable;
            private ListView mListView;
            private SimpleCursorAdapter mAdapter;
            private ThemeSelectionDialog themeDialog;

            /**
             * 当Android从头开始启动此Activity时调用onCreate
             */
            @Override
            protected void onCreate(Bundle savedInstanceState) {
                // 首先应用主题，然后调用父类的onCreate
                applyTheme();
                super.onCreate(savedInstanceState);

                setContentView(R.layout.notes_list_layout); // 需要创建这个布局文件

                // 用户不需要按住键来使用菜单快捷键
                // setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT); // 这行在AppCompatActivity中不需要

                /* 如果启动此Activity的Intent中没有提供数据，那么此Activity
                 * 是在意图过滤器匹配MAIN操作时启动的。我们应该使用默认的提供者URI。
                 */
                // 获取启动此Activity的意图
                Intent intent = getIntent();

                // 如果Intent没有关联数据，则将数据设置为默认URI，用于访问笔记列表
                if (intent.getData() == null) {
                    intent.setData(NotePad.Notes.CONTENT_URI);
                }

                // 初始化ListView
                mListView = findViewById(R.id.list_view);

                /*
                 * 为ListView设置上下文菜单激活的回调。监听器设置为
                 * 此Activity。效果是为ListView中的项目启用上下文菜单，
                 * 并且上下文菜单由NotesList中的方法处理。
                 */
                mListView.setOnCreateContextMenuListener(this);

                /* 执行托管查询。Activity在需要时处理关闭和重新查询游标
                 */
                Cursor cursor = getContentResolver().query(
                        getIntent().getData(),
                        PROJECTION,
                        null,
                        null,
                        NotePad.Notes.DEFAULT_SORT_ORDER
                );

                /*
                 * 以下两个数组在游标中的列和ListView中项目的视图ID之间创建"映射"
                 * dataColumns数组中的每个元素代表一个列名；
                 * viewID数组中的每个元素代表一个视图的ID。
                 * SimpleCursorAdapter按升序映射它们以确定每个列值将出现在ListView中的位置。
                 */

                // 要在视图中显示的游标列的名称，初始化为标题列
                String[] dataColumns = {
                        NotePad.Notes.COLUMN_NAME_TITLE,
                        NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                        NotePad.Notes.COLUMN_NAME_NOTE,  // 添加笔记内容用于预览
                        NotePad.Notes.COLUMN_NAME_TAG_ID  // 标签ID
                };

                // 将显示游标列的视图ID，初始化为noteslist_item.xml中的TextView
                int[] viewIDs = {
                        android.R.id.text1,
                        R.id.tv_time,
                        R.id.tv_preview,
                        R.id.tv_tag          // 只保留一个标签文本视图
                };

                // 创建适配器
                mAdapter = new SimpleCursorAdapter(
                        this,
                        R.layout.noteslist_item,
                        cursor,
                        dataColumns,
                        viewIDs
                );

                // 设置自定义的ViewBinder来处理时间显示逻辑
                mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                    @Override
                    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                        if (view.getId() == R.id.tv_time) {
                            long modificationDate = cursor.getLong(COLUMN_INDEX_MODIFICATION_DATE);
                            String timeText = formatTimestamp(modificationDate);
                            TextView timeView = (TextView) view;
                            timeView.setText(timeText);
                            return true;
                        } else if (view.getId() == R.id.tv_preview) {
                            String noteContent = cursor.getString(COLUMN_INDEX_NOTE);
                            if (noteContent != null && noteContent.length() > 100) {
                                noteContent = noteContent.substring(0, 100) + "...";
                            }
                            TextView previewView = (TextView) view;
                            previewView.setText(noteContent != null ? noteContent : "");
                            return true;
                        } else if (view.getId() == R.id.tag_indicator || view.getId() == R.id.tv_tag) {
                            // 两个标签视图都使用同一个标签ID列
                            long tagId = cursor.getLong(COLUMN_INDEX_TAG_ID);

                            if (view.getId() == R.id.tag_indicator) {
                                View tagView = view;
                                if (tagId > 0) {
                                    Tag tag = getTagFromCache(tagId);
                                    if (tag != null) {
                                        tagView.setBackgroundColor(tag.getColor());
                                        tagView.setVisibility(View.VISIBLE);
                                    } else {
                                        tagView.setVisibility(View.INVISIBLE);
                                    }
                                } else {
                                    tagView.setVisibility(View.INVISIBLE);
                                }
                                return true;
                            } else if (view.getId() == R.id.tv_tag) {
                                TextView tagTextView = (TextView) view;
                                if (tagId > 0) {
                                    Tag tag = getTagFromCache(tagId);
                                    if (tag != null) {
                                        tagTextView.setText(tag.getName());
                                        tagTextView.setBackgroundColor(tag.getColor());
                                        int textColor = getContrastColor(tag.getColor());
                                        tagTextView.setTextColor(textColor);
                                        tagTextView.setVisibility(View.VISIBLE);
                                    } else {
                                        tagTextView.setVisibility(View.GONE);
                                    }
                                } else {
                                    tagTextView.setVisibility(View.GONE);
                                }
                                return true;
                            }
                        }
                        return false;
                    }
                });

                mListView.setAdapter(mAdapter);

                // 设置列表项点击监听器
                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        onListItemClick((ListView) parent, view, position, id);
                    }
                });
            }
            private int getContrastColor(int color) {
                // 计算颜色的亮度
                double luminance = (0.299 * Color.red(color) +
                        0.587 * Color.green(color) +
                        0.114 * Color.blue(color)) / 255;

                // 如果亮度大于0.5，使用黑色文字，否则使用白色文字
                return luminance > 0.5 ? Color.BLACK : Color.WHITE;
            }

            /**
             * 格式化时间戳为易读的字符串
             */
            private String formatTimestamp(long timestamp) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }

            /*
             * 从xml资源加载菜单，添加其他应用的替代操作，返回true显示菜单
             */
            @Override
            public boolean onCreateOptionsMenu(Menu menu) {
                // 从XML资源加载菜单
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.list_options_menu, menu);

                // 生成可以在整个列表上执行的任何其他操作。
                // 在正常安装中，这里没有找到其他操作，
                // 但这允许其他应用程序使用它们自己的操作扩展我们的菜单。
                Intent intent = new Intent(null, getIntent().getData());
                intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                        new ComponentName(this, NotesList.class), null, intent, 0, null);

                // 初始化搜索功能
                MenuItem searchItem = menu.findItem(R.id.menu_search);
                if (searchItem != null) {
                    SearchView searchView = (SearchView) searchItem.getActionView();
                    if (searchView != null) {
                        searchView.setQueryHint(getString(R.string.search_hint));

                        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                            @Override
                            public boolean onQueryTextSubmit(String query) {
                                performSearch(query);
                                searchView.clearFocus();
                                return true;
                            }

                            @Override
                            public boolean onQueryTextChange(String newText) {
                                if (searchHandler != null) {
                                    searchHandler.removeCallbacks(searchRunnable);
                                }
                                searchRunnable = () -> performSearch(newText);
                                searchHandler.postDelayed(searchRunnable, 500);
                                return true;
                            }
                        });

                        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                            @Override
                            public boolean onClose() {
                                refreshNoteList();
                                return false;
                            }
                        });
                    }
                }
                MenuItem themeItem = menu.findItem(R.id.menu_theme);
                if (themeItem != null) {
                    themeItem.setOnMenuItemClickListener(item -> {
                        showThemeSelectionDialog();
                        return true;
                    });
                }
                MenuItem filterTagItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "标签筛选");
                filterTagItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                return super.onCreateOptionsMenu(menu);
            }

            private void performSearch(String query) {
                if (query == null || query.trim().isEmpty()) {
                    refreshNoteList();
                    return;
                }

                String selection;
                String[] selectionArgs;

                if (mCurrentFilterTagId != -1) {
                    selection = "(" + NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                            NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?) AND " +
                            NotePad.Notes.COLUMN_NAME_TAG_ID + " = ?";
                    selectionArgs = new String[]{"%" + query + "%", "%" + query + "%",
                            String.valueOf(mCurrentFilterTagId)};
                } else {
                    selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                            NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
                    selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
                }

                Cursor cursor = getContentResolver().query(
                        getIntent().getData(),
                        PROJECTION,
                        selection,
                        selectionArgs,
                        NotePad.Notes.DEFAULT_SORT_ORDER
                );

                mAdapter.changeCursor(cursor);

                if (cursor != null && cursor.getCount() == 0) {
                    Toast.makeText(this, R.string.no_search_results, Toast.LENGTH_SHORT).show();
                }
            }

            /**
             * 刷新笔记列表，显示所有笔记
             */
            private void refreshNoteList() {
                String selection = null;
                String[] selectionArgs = null;

                if (mCurrentFilterTagId != -1) {
                    selection = NotePad.Notes.COLUMN_NAME_TAG_ID + " = ?";
                    selectionArgs = new String[]{String.valueOf(mCurrentFilterTagId)};
                }

                Cursor cursor = getContentResolver().query(
                        getIntent().getData(),
                        PROJECTION,
                        selection,
                        selectionArgs,
                        NotePad.Notes.DEFAULT_SORT_ORDER
                );

                mAdapter.changeCursor(cursor);
            }


            /*
             * 检查剪贴板的内容 启用/禁用粘贴选项，若有选中项则生成编辑操作，动态更新菜单状态
             */
            @Override
            public boolean onPrepareOptionsMenu(Menu menu) {
                super.onPrepareOptionsMenu(menu);

                // 获取当前显示的笔记数量
                final boolean haveItems = mAdapter.getCount() > 0;

                // 如果列表中有任何笔记（这意味着其中一个被选中），
                // 那么我们需要生成可以在当前选择上执行的操作。
                // 这将是我们自己的特定操作以及可以找到的任何扩展的组合。
                if (haveItems && mListView.getSelectedItemId() != AdapterView.INVALID_ROW_ID) {

                    // 这是选中的项目
                    Uri uri = ContentUris.withAppendedId(getIntent().getData(), mListView.getSelectedItemId());

                    // 创建一个包含一个元素的Intent数组。这将用于根据选定的菜单项发送Intent
                    Intent[] specifics = new Intent[1];

                    // 将数组中的Intent设置为对所选笔记URI的EDIT操作
                    specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

                    // 创建一个包含一个元素的菜单项数组。这将包含EDIT选项
                    MenuItem[] items = new MenuItem[1];

                    // 创建一个没有特定操作的Intent，使用所选笔记的URI
                    Intent intent = new Intent(null, uri);

                    /* 将ALTERNATIVE类别添加到Intent，使用笔记ID URI作为其数据。这准备Intent作为在菜单中分组替代选项的位置。
                     */
                    intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

                    /*
                     * 向菜单添加替代选项
                     */
                    menu.addIntentOptions(
                            Menu.CATEGORY_ALTERNATIVE,  // 将Intent作为替代组中的选项添加
                            Menu.NONE,                  // 不需要唯一的项目ID
                            Menu.NONE,                  // 替代选项不需要按顺序排列
                            null,                       // 不将调用者名称从组中排除
                            specifics,                  // 这些特定选项必须首先出现
                            intent,                     // 这些Intent对象映射到specifics中的选项
                            Menu.NONE,                  // 不需要标志
                            items                       // 从specifics到Intent映射生成的菜单项
                    );

                    // 如果编辑菜单项存在，为其添加快捷键
                    if (items[0] != null) {
                        // 将编辑菜单项快捷键设置为数字"1"，字母"e"
                        items[0].setShortcut('1', 'e');
                    }
                } else {
                    // 如果列表为空，从菜单中移除任何现有的替代操作
                    menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
                }

                // 显示菜单
                return true;
            }

            /*
             * 处理添加笔记操作 其他操作交给父类处理
             */
            @Override
            public boolean onOptionsItemSelected(MenuItem item) {
                if (item.getItemId() == R.id.menu_add) {
                    /*
                     * 使用Intent启动新Activity。Activity的意图过滤器
                     * 必须具有ACTION_INSERT操作。未设置类别，因此假定为DEFAULT。
                     * 实际上，这会在NotePad中启动NoteEditor Activity。
                     */
                    startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
                    return true;
                }
                if (item.getTitle() != null &&
                        item.getTitle().toString().equals("标签筛选")) {
                    showTagFilterDialog();
                    return true;
                }
                return super.onOptionsItemSelected(item);
            }

            /**
             * 当用户在列表中上下文点击笔记时调用此方法。NotesList将其自身注册
             * 为ListView中上下文菜单的处理程序（这是在onCreate()中完成的）。
             * <p>
             * 唯一可用的选项是COPY和DELETE。
             * <p>
             * 上下文点击相当于长按。
             */
            @Override
            public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

                // 菜单项中的数据
                AdapterView.AdapterContextMenuInfo info;

                // 尝试获取长按的ListView中项目的位置
                try {
                    // 将传入的数据对象转换为AdapterView对象的类型
                    info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                } catch (ClassCastException e) {
                    // 如果菜单对象无法转换，记录错误
                    Log.e(TAG, "bad menuInfo", e);
                    return;
                }

                /*
                 * 获取选定位置处项目关联的数据。getItem()返回
                 * ListView的后备适配器与项目关联的任何数据。在NotesList中，
                 * 适配器将笔记的所有数据与其列表项关联。因此，
                 * getItem()将该数据作为Cursor返回。
                 */
                Cursor cursor = (Cursor) mAdapter.getItem(info.position);

                // 如果游标为空，则由于某种原因适配器无法从提供者获取数据，因此向调用者返回null
                if (cursor == null) {
                    // 由于某种原因请求的项目不可用，不执行任何操作
                    return;
                }

                // 从XML资源加载菜单
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.list_context_menu, menu);

                // 将菜单标题设置为所选笔记的标题
                menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

                // 附加到菜单项，用于任何其他可以对其进行操作的活动。
                // 这会在系统上查询任何对我们的数据实现ALTERNATIVE_ACTION的活动，
                // 为找到的每个活动添加一个菜单项。
                Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                        Integer.toString((int) info.id)));
                intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                        new ComponentName(this, NotesList.class), null, intent, 0, null);
            }

            /**
             * 当用户从上下文菜单中选择项目时调用此方法
             * (参见onCreateContextMenu())。实际处理的唯一菜单项是DELETE和
             * COPY。其他任何内容都是替代选项，应进行默认处理。
             */
            @Override
            public boolean onContextItemSelected(MenuItem item) {
                // 菜单项中的数据
                AdapterView.AdapterContextMenuInfo info;

                /*
                 * 从菜单项中获取额外信息。当笔记列表中的笔记被长按时，
                 * 会出现上下文菜单。菜单的菜单项自动获取与长按的笔记关联的数据。
                 * 数据来自支持列表的提供者。
                 *
                 * 笔记的数据在ContextMenuInfo对象中传递给上下文菜单创建例程。
                 *
                 * 当单击上下文菜单项之一时，相同的数据与笔记ID一起通过item参数传递给onContextItemSelected()。
                 */
                try {
                    // 将item中的数据对象转换为AdapterView对象的类型
                    info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                } catch (ClassCastException e) {

                    // 如果对象无法转换，记录错误
                    Log.e(TAG, "bad menuInfo", e);

                    // 触发菜单项的默认处理
                    return false;
                }

                // 将所选笔记的ID附加到随传入Intent发送的URI
                Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

                /*
                 * 获取菜单项的ID并将其与已知操作进行比较
                 */
                int id = item.getItemId();
                if (id == R.id.context_open) {
                    // 启动活动以查看/编辑当前选定的项目
                    startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
                    return true;
                } else if (id == R.id.context_copy) {
                    // 获取剪贴板服务的句柄
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);

                    // 将笔记URI复制到剪贴板。实际上，这复制了笔记本身
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newUri(   // 保存URI的新剪贴板项
                                getContentResolver(),               // 用于检索URI信息的解析器
                                "Note",                             // 剪贴板的标签
                                noteUri));                          // URI
                    }
                    // 返回调用者并跳过进一步处理
                    return true;
                } else if (id == R.id.context_delete) {
                    // 通过传入笔记ID格式的URI从提供者中删除笔记
                    getContentResolver().delete(
                            noteUri,  // 提供者的URI
                            null,     // 不需要where子句，因为只传入单个笔记ID
                            null      // 不使用where子句，因此不需要where参数
                    );

                    // 返回调用者并跳过进一步处理
                    return true;
                } else if (item.getTitle().toString().equals(getString(R.string.resolve_title))) {
                    // 编辑标题
                    TitleEditor editor = TitleEditor.newInstance(noteUri);
                    editor.show(getSupportFragmentManager(), "title_editor");
                    return true;
                }
                return super.onContextItemSelected(item);
            }

            /**
             * 当用户单击显示列表中的笔记时调用此方法
             * <p>
             * 此方法处理PICK（从提供者获取数据）或GET_CONTENT（获取或创建数据）的传入操作。
             * 如果传入操作是EDIT，此方法发送新的Intent以启动NoteEditor。
             */
            protected void onListItemClick(ListView l, View v, int position, long id) {

                // 从传入URI和行ID构造新URI
                Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

                // 从传入Intent获取操作
                String action = getIntent().getAction();

                // 处理笔记数据请求
                if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

                    // 设置要返回给调用此Activity的组件的结果
                    // 结果包含新URI
                    setResult(RESULT_OK, new Intent().setData(uri));
                } else {

                    // 发送Intent以启动可以处理ACTION_EDIT的Activity
                    // Intent的数据是笔记ID URI。效果是调用NoteEdit
                    startActivity(new Intent(Intent.ACTION_EDIT, uri));
                }
            }

            // 添加获取适配器的方法
            public SimpleCursorAdapter getListAdapter() {
                return mAdapter;
            }

            // 添加获取选中项ID的方法
            public long getSelectedItemId() {
                return mListView.getSelectedItemId();
            }

            private void showThemeSelectionDialog() {
                themeDialog = new ThemeSelectionDialog();
                themeDialog.setOnThemeChangeListener(themeIndex -> {
                    // 重启Activity以应用新主题
                    recreate();
                });
                themeDialog.show(getSupportFragmentManager(), "theme_dialog");
            }

            private void applyTheme() {
                ThemeManager.applyTheme(this);
            }

            // 添加标签缓存方法
            private Tag getTagFromCache(long tagId) {
                if (mTagCache.containsKey(tagId)) {
                    return mTagCache.get(tagId);
                }

                // 从数据库查询
                Uri tagUri = ContentUris.withAppendedId(NotePad.Notes.TAG_ID_URI_BASE, tagId);
                Cursor cursor = getContentResolver().query(
                        tagUri,
                        new String[]{
                                NotePad.Notes.COLUMN_NAME_TAG_NAME,
                                NotePad.Notes.COLUMN_NAME_TAG_COLOR
                        },
                        null,
                        null,
                        null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    Tag tag = new Tag();
                    tag.setId(tagId);
                    tag.setName(cursor.getString(0));
                    tag.setColor(cursor.getInt(1));
                    mTagCache.put(tagId, tag);
                    cursor.close();
                    return tag;
                }

                return null;
            }

            private void showTagFilterDialog() {
                androidx.appcompat.app.AlertDialog.Builder builder =
                        new androidx.appcompat.app.AlertDialog.Builder(this);
                builder.setTitle(R.string.tag_filter);

                // 加载所有标签
                final List<Tag> tags = new ArrayList<>();
                Cursor cursor = getContentResolver().query(
                        NotePad.Notes.TAGS_CONTENT_URI,
                        new String[]{"_id", "name", "color"},
                        null,
                        null,
                        "name ASC"
                );

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        Tag tag = new Tag();
                        tag.setId(cursor.getLong(0));
                        tag.setName(cursor.getString(1));
                        tag.setColor(cursor.getInt(2));
                        tags.add(tag);
                    }
                    cursor.close();
                }

                // 添加"全部"选项
                Tag allTag = new Tag();
                allTag.setId(-1);
                allTag.setName(getString(R.string.all_notes));
                tags.add(0, allTag);

                // 添加"无标签"选项
                Tag noTag = new Tag();
                noTag.setId(0);
                noTag.setName(getString(R.string.tag_none));
                tags.add(1, noTag);

                String[] tagNames = new String[tags.size()];
                for (int i = 0; i < tags.size(); i++) {
                    tagNames[i] = tags.get(i).getName();
                }

                int selectedIndex = 0;
                for (int i = 0; i < tags.size(); i++) {
                    if (tags.get(i).getId() == mCurrentFilterTagId) {
                        selectedIndex = i;
                        break;
                    }
                }

                builder.setSingleChoiceItems(tagNames, selectedIndex, (dialog, which) -> {
                    Tag selectedTag = tags.get(which);
                    mCurrentFilterTagId = selectedTag.getId();

                    // 重新加载笔记列表
                    refreshNoteList();

                    // 更新标题显示当前筛选条件
                    if (mCurrentFilterTagId == -1) {
                        setTitle(getString(R.string.all_notes));
                    } else {
                        setTitle(getString(R.string.tag_filter_prefix) + selectedTag.getName());
                    }

                    dialog.dismiss();
                });

                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
            }

        }
