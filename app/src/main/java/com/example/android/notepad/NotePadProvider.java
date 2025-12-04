
        /*
         * Copyright (C) 2007 The Android Open Source Project
         *
         * 根据 Apache License, Version 2.0 (the "License") 授权;
         * 除非符合许可证要求，否则您不得使用此文件。
         * 您可以在以下网址获取许可证副本：
         *
         *      http://www.apache.org/licenses/LICENSE-2.0
         *
         * 除非适用法律要求或书面同意，软件
         * 根据许可证按"原样"分发，
         * 没有任何明示或暗示的担保或条件。
         * 请参阅许可证了解特定语言规定的权限和
         * 限制。
         */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.ContentProvider.PipeDataWriter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * 提供对笔记数据库的访问。每个笔记有一个标题、笔记内容、创建日期和修改日期。
 */
public class NotePadProvider extends ContentProvider implements PipeDataWriter<Cursor> {
    // 用于调试和日志记录
    private static final String TAG = "NotePadProvider";
    private static final int DEFAULT_TAG_COLOR =  0xFFE0E0E0;
    /**
     * 提供程序用作其底层数据存储的数据库
     */
    private static final String DATABASE_NAME = "note_pad.db";

    /**
     * 数据库版本
     */
    private static final int DATABASE_VERSION = 3;

    /**
     * 用于从数据库中选择列的投影映射
     */
    private static HashMap<String, String> sNotesProjectionMap;

    /**
     * 用于从数据库中选择列的投影映射
     */
    private static HashMap<String, String> sLiveFolderProjectionMap;

    /**
     * 标准投影，用于普通笔记的相关列。
     */
    private static final String[] READ_NOTE_PROJECTION = new String[] {
            NotePad.Notes._ID,               // 投影位置 0，笔记的 id
            NotePad.Notes.COLUMN_NAME_NOTE,  // 投影位置 1，笔记的内容
            NotePad.Notes.COLUMN_NAME_TITLE, // 投影位置 2，笔记的标题
    };
    private static final int READ_NOTE_NOTE_INDEX = 1;
    private static final int READ_NOTE_TITLE_INDEX = 2;

    /*
     * Uri 匹配器使用的常量，用于根据传入 URI 的模式选择操作
     */
    // 传入的 URI 匹配 Notes URI 模式
    private static final int NOTES = 1;

    // 传入的 URI 匹配 Note ID URI 模式
    private static final int NOTE_ID = 2;

    // 传入的 URI 匹配 Live Folder URI 模式
    private static final int LIVE_FOLDER_NOTES = 3;

    /**
     * UriMatcher 实例
     */
    private static final UriMatcher sUriMatcher;

    // 新 DatabaseHelper 的句柄。
    private DatabaseHelper mOpenHelper;

    private static HashMap<String,String> sTagsProjectionMap;


    /**
     * 实例化并设置静态对象的代码块
     */
    static {

        /*
         * 创建并初始化 URI 匹配器
         */
        // 创建新实例
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // 添加一个模式，将以 "notes" 结尾的 URI 路由到 NOTES 操作
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);

        // 添加一个模式，将以 "notes" 加一个整数结尾的 URI
        // 路由到笔记 ID 操作
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);

        // 添加一个模式，将以 live_folders/notes 结尾的 URI 路由到
        // live folder 操作
        sUriMatcher.addURI(NotePad.AUTHORITY, "live_folders/notes", LIVE_FOLDER_NOTES);

        sUriMatcher.addURI(NotePad.AUTHORITY, "tags", 10); // 标签列表
        sUriMatcher.addURI(NotePad.AUTHORITY, "tags/#", 11); // 单个标签
        /*
         * 创建并初始化一个返回所有列的投影映射
         */

        // 创建一个新的投影映射实例。该映射给定一个字符串返回一个列名。
        // 两者通常是相等的。
        sNotesProjectionMap = new HashMap<String, String>();

        // 将字符串 "_ID" 映射到列名 "_ID"
        sNotesProjectionMap.put(NotePad.Notes._ID, NotePad.Notes._ID);

        // 将 "title" 映射到 "title"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_TITLE);

        // 将 "note" 映射到 "note"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_NOTE);

//        // 将 "created" 映射到 "created"
//        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE,
//                NotePad.Notes.COLUMN_NAME_CREATE_DATE);

        // 将 "modified" 映射到 "modified"
        sNotesProjectionMap.put(
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);

        /*
         * 创建并初始化用于处理 Live Folders 的投影映射
         */

        // 创建一个新的投影映射实例
        sLiveFolderProjectionMap = new HashMap<String, String>();

        // 对于 live folder，将 "_ID" 映射到 "_ID AS _ID"
        sLiveFolderProjectionMap.put(LiveFolders._ID, NotePad.Notes._ID + " AS " + LiveFolders._ID);

        // 将 "NAME" 映射到 "title AS NAME"
        sLiveFolderProjectionMap.put(LiveFolders.NAME, NotePad.Notes.COLUMN_NAME_TITLE + " AS " +
                LiveFolders.NAME);

        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TAG_ID, NotePad.Notes.COLUMN_NAME_TAG_ID);

        // 添加标签投影映射
        sTagsProjectionMap = new HashMap<String, String>();
        sTagsProjectionMap.put("_id", "_id");
        sTagsProjectionMap.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, NotePad.Notes.COLUMN_NAME_TAG_NAME);
        sTagsProjectionMap.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, NotePad.Notes.COLUMN_NAME_TAG_COLOR);
        sTagsProjectionMap.put("created_date", "created_date");
    }

    /**
     *
     * 此类帮助打开、创建和升级数据库文件。设置为包可见性以便测试。
     */
    static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {

            // 调用超类构造函数，请求默认的游标工厂。
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         *
         * 使用从 NotePad 类获取的表名和列名创建底层数据库。
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            // 创建 notes 表 - 确保包含 tag_id 列
            db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_TAG_ID + " INTEGER DEFAULT 0"
                    + ");");

            // 创建 tags 表
            db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME_TAGS + " ("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + NotePad.Notes.COLUMN_NAME_TAG_NAME + " TEXT NOT NULL,"  // 使用常量
                    + NotePad.Notes.COLUMN_NAME_TAG_COLOR + " INTEGER DEFAULT " + DEFAULT_TAG_COLOR + ","  // 使用常量
                    + "created_date INTEGER"
                    + ");");

            // 插入默认标签
            ContentValues values = new ContentValues();
            long now = System.currentTimeMillis();

            values.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, "工作");
            values.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, 0xFF4CAF50);
            values.put("created_date", now);
            db.insert(NotePad.Notes.TABLE_NAME_TAGS, null, values);

            values.clear();
            values.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, "个人");
            values.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, 0xFF2196F3);
            values.put("created_date", now);
            db.insert(NotePad.Notes.TABLE_NAME_TAGS, null, values);

            values.clear();
            values.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, "重要");
            values.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, 0xFFFF9800);
            values.put("created_date", now);
            db.insert(NotePad.Notes.TABLE_NAME_TAGS, null, values);
        }

        /**
         *
         * 演示当底层数据存储更改时提供程序必须考虑的情况。
         * 在此示例中，通过销毁现有数据来升级数据库。
         * 实际应用程序应就地升级数据库。
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            Log.w(TAG, "正在从版本 " + oldVersion + " 升级数据库到 " + newVersion);

            if (oldVersion < 3) { // 新版本号为3
                try {
                    // 添加标签相关列 - 使用IF NOT EXISTS避免重复添加
                    db.execSQL("ALTER TABLE " + NotePad.Notes.TABLE_NAME +
                            " ADD COLUMN " + NotePad.Notes.COLUMN_NAME_TAG_ID + " INTEGER DEFAULT 0");
                } catch (SQLException e) {
                    Log.w(TAG, "添加tag_id列时出错，可能已经存在: " + e.getMessage());
                }

                // 创建标签表 - 先检查是否已存在
                Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                        new String[] {NotePad.Notes.TABLE_NAME_TAGS});
                if (cursor != null) {
                    if (!cursor.moveToFirst()) {
                        // 表不存在，创建它
                        db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME_TAGS + " ("
                                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + NotePad.Notes.COLUMN_NAME_TAG_NAME + " TEXT NOT NULL,"
                                + NotePad.Notes.COLUMN_NAME_TAG_COLOR + " INTEGER DEFAULT " + DEFAULT_TAG_COLOR + ","
                                + "created_date INTEGER"
                                + ");");
                    }
                    cursor.close();
                }

                // 检查是否已有默认标签，如果没有则插入
                cursor = db.rawQuery("SELECT COUNT(*) FROM " + NotePad.Notes.TABLE_NAME_TAGS, null);
                int count = 0;
                if (cursor != null && cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                    cursor.close();
                }

                if (count == 0) {
                    ContentValues values = new ContentValues();
                    long now = System.currentTimeMillis();

                    // 插入默认标签 - 工作
                    values.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, "工作");
                    values.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, 0xFF4CAF50);
                    values.put("created_date", now);
                    db.insert(NotePad.Notes.TABLE_NAME_TAGS, null, values);

                    // 插入默认标签 - 个人
                    values.clear();
                    values.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, "个人");
                    values.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, 0xFF2196F3);
                    values.put("created_date", now);
                    db.insert(NotePad.Notes.TABLE_NAME_TAGS, null, values);

                    // 插入默认标签 - 重要
                    values.clear();
                    values.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, "重要");
                    values.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, 0xFFFF9800);
                    values.put("created_date", now);
                    db.insert(NotePad.Notes.TABLE_NAME_TAGS, null, values);
                }
            }
        }
    }

    /**
     *
     * 通过创建新的 DatabaseHelper 来初始化提供程序。当 Android 响应客户端的解析器请求创建提供程序时，会自动调用 onCreate()。
     */
    @Override
    public boolean onCreate() {

        // 创建一个新的辅助对象。请注意，数据库本身直到有东西尝试访问它时才会打开，
        // 并且仅当它尚不存在时才会创建。
        mOpenHelper = new DatabaseHelper(getContext());

        // 假设任何失败都会通过抛出的异常报告。
        return true;
    }

    /**
     * 当客户端调用
     * {@link android.content.ContentResolver#query(Uri, String[], String, String[], String)} 时调用此方法。
     * 查询数据库并返回包含结果的游标。
     *
     * @return 包含查询结果的游标。如果查询没有结果或发生异常，则游标存在但为空。
     * @throws IllegalArgumentException 如果传入的 URI 模式无效。
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // 构造一个新的查询构建器并设置其表名
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(NotePad.Notes.TABLE_NAME);

        /**
         * 根据 URI 模式匹配选择投影并调整 "where" 子句。
         */
        switch (sUriMatcher.match(uri)) {
            // 如果传入的 URI 是针对 notes，选择 Notes 投影
            case NOTES:
                qb.setProjectionMap(sNotesProjectionMap);
                break;

            /* 如果传入的 URI 是针对由其 ID 标识的单个笔记，选择
             * 笔记 ID 投影，并将 "_ID = <noteID>" 附加到 where 子句，以便
             * 它选择那个单个笔记
             */
            case NOTE_ID:
                qb.setProjectionMap(sNotesProjectionMap);
                qb.appendWhere(
                        NotePad.Notes._ID +    // ID 列的名称
                                "=" +
                                // 笔记 ID 本身在传入 URI 中的位置
                                uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION));
                break;

            case LIVE_FOLDER_NOTES:
                // 如果传入的 URI 来自 live folder，选择 live folder 投影。
                qb.setProjectionMap(sLiveFolderProjectionMap);
                break;

            case 10: // 标签列表
                qb.setTables(NotePad.Notes.TABLE_NAME_TAGS);
                qb.setProjectionMap(sTagsProjectionMap);
                break;

            case 11: // 单个标签
                qb.setTables(NotePad.Notes.TABLE_NAME_TAGS);
                qb.setProjectionMap(sTagsProjectionMap);
                qb.appendWhere("_id = " + uri.getLastPathSegment());
                break;

            default:
                // 如果 URI 不匹配任何已知模式，抛出异常。
                throw new IllegalArgumentException("未知 URI " + uri);
        }


        String orderBy;
// 根据 URI 类型决定排序方式
        switch (sUriMatcher.match(uri)) {
            case 10: // 标签列表
            case 11: // 单个标签
                // 标签表使用创建日期排序
                if (TextUtils.isEmpty(sortOrder)) {
                    orderBy = "created_date ASC";
                } else {
                    orderBy = sortOrder;
                }
                break;
            default:
                // 笔记表使用默认排序
                if (TextUtils.isEmpty(sortOrder)) {
                    orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
                } else {
                    orderBy = sortOrder;
                }
                break;
        }

        // 以"读取"模式打开数据库对象，因为不需要进行写入。
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        /*
         * 执行查询。如果尝试读取数据库时没有问题，则返回一个 Cursor
         * 对象；否则，游标变量包含 null。如果没有选择任何记录，则 Cursor 对象为空，并且 Cursor.getCount() 返回 0。
         */
        Cursor c = qb.query(
                db,            // 要查询的数据库
                projection,    // 要从查询返回的列
                selection,     // where 子句的列
                selectionArgs, // where 子句的值
                null,          // 不对行进行分组
                null,          // 不按行组过滤
                orderBy        // 排序顺序
        );

        // 告诉 Cursor 监视哪个 URI，以便它知道其源数据何时更改
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /**
     * 当客户端调用 {@link android.content.ContentResolver#getType(Uri)} 时调用此方法。
     * 返回给定作为参数的 URI 的 MIME 数据类型。
     *
     * @param uri 需要 MIME 类型的 URI。
     * @return URI 的 MIME 类型。
     * @throws IllegalArgumentException 如果传入的 URI 模式无效。
     */
    @Override
    public String getType(Uri uri) {

        /**
         * 根据传入的 URI 模式选择 MIME 类型
         */
        switch (sUriMatcher.match(uri)) {

            // 如果模式是针对 notes 或 live folders，返回通用内容类型。
            case NOTES:
            case LIVE_FOLDER_NOTES:
                return NotePad.Notes.CONTENT_TYPE;

            // 如果模式是针对笔记 ID，返回笔记 ID 内容类型。
            case NOTE_ID:
                return NotePad.Notes.CONTENT_ITEM_TYPE;

            // 如果 URI 模式不匹配任何允许的模式，抛出异常。
            default:
                throw new IllegalArgumentException("未知 URI " + uri);
        }
    }

//BEGIN_INCLUDE(stream)
    /**
     * 这描述了支持以流形式打开笔记 URI 的 MIME 类型。
     */
    static ClipDescription NOTE_STREAM_TYPES = new ClipDescription(null,
            new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN });

    /**
     * 返回可用数据流的类型。支持指向特定笔记的 URI。
     * 应用程序可以将这样的笔记转换为纯文本流。
     *
     * @param uri 要分析的 URI
     * @param mimeTypeFilter 要检查的 MIME 类型。此方法仅返回匹配过滤器的 MIME 类型的数据流类型。当前，仅匹配 text/plain MIME 类型。
     * @return 数据流 MIME 类型。当前，仅返回 text/plain。
     * @throws IllegalArgumentException 如果 URI 模式不匹配任何支持的模式。
     */
    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        /**
         *  根据传入的 URI 模式选择数据流类型。
         */
        switch (sUriMatcher.match(uri)) {

            // 如果模式是针对 notes 或 live folders，返回 null。数据流不支持此类 URI。
            case NOTES:
            case LIVE_FOLDER_NOTES:
                return null;

            // 如果模式是针对笔记 ID 并且 MIME 过滤器是 text/plain，则返回 text/plain
            case NOTE_ID:
                return NOTE_STREAM_TYPES.filterMimeTypes(mimeTypeFilter);

            // 如果 URI 模式不匹配任何允许的模式，抛出异常。
            default:
                throw new IllegalArgumentException("未知 URI " + uri);
        }
    }


    /**
     * 为每个支持的流类型返回数据流。此方法对传入 URI 执行查询，然后使用
     * {@link android.content.ContentProvider#openPipeHelper(Uri, String, Bundle, Object,
     * PipeDataWriter)} 启动另一个线程以将数据转换为流。
     *
     * @param uri 指向数据流的 URI 模式
     * @param mimeTypeFilter 包含 MIME 类型的字符串。此方法尝试获取具有此 MIME 类型的数据流。
     * @param opts 调用者提供的附加选项。可以根据内容提供者的意愿进行解释。
     * @return AssetFileDescriptor 文件的句柄。
     * @throws FileNotFoundException 如果传入的 URI 没有关联的文件。
     */
    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {

        // 检查 MIME 类型过滤器是否匹配支持的 MIME 类型。
        String[] mimeTypes = getStreamTypes(uri, mimeTypeFilter);

        // 如果 MIME 类型被支持
        if (mimeTypes != null) {

            // 检索此 URI 的笔记。使用为此提供程序定义的查询方法，
            // 而不是使用数据库查询方法。
            Cursor c = query(
                    uri,                    // 笔记的 URI
                    READ_NOTE_PROJECTION,   // 获取包含笔记 ID、标题和内容的投影
                    null,                   // 没有 WHERE 子句，获取所有匹配记录
                    null,                   // 因为没有 WHERE 子句，所以没有选择条件
                    null                    // 使用默认排序顺序（修改日期，降序）
            );


            // 如果查询失败或游标为空，停止
            if (c == null || !c.moveToFirst()) {

                // 如果游标为空，只需关闭游标并返回
                if (c != null) {
                    c.close();
                }

                // 如果游标为 null，抛出异常
                throw new FileNotFoundException("无法查询 " + uri);
            }

            // 启动一个新线程，将流数据管道传输回调用者。
            return new AssetFileDescriptor(
                    openPipeHelper(uri, mimeTypes[0], opts, c, this), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH);
        }

        // 如果 MIME 类型不被支持，返回文件的只读句柄。
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    /**
     * {@link android.content.ContentProvider.PipeDataWriter} 的实现
     * 以执行将游标中的数据转换为客户端可读取的数据流的实际工作。
     */
    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType,
                                Bundle opts, Cursor c) {
        // 我们目前仅支持从单个笔记条目转换为文本，
        // 因此此处无需进行游标数据类型检查。
        FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
            pw.println(c.getString(READ_NOTE_TITLE_INDEX));
            pw.println("");
            pw.println(c.getString(READ_NOTE_NOTE_INDEX));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "糟糕", e);
        } finally {
            c.close();
            if (pw != null) {
                pw.flush();
            }
            try {
                fout.close();
            } catch (IOException e) {
            }
        }
    }
//END_INCLUDE(stream)

    /**
     * 当客户端调用
     * {@link android.content.ContentResolver#insert(Uri, ContentValues)} 时调用此方法。
     * 向数据库插入新行。此方法为任何未包含在传入映射中的列设置默认值。
     * 如果插入了行，则通知监听器更改。
     * @return 插入行的行 ID。
     * @throws SQLException 如果插入失败。
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) == 10) { // 插入标签
            ContentValues values;
            if (initialValues != null) {
                values = new ContentValues(initialValues);
            } else {
                values = new ContentValues();
            }

            // 设置默认值
            Long now = Long.valueOf(System.currentTimeMillis());
            if (!values.containsKey("created_date")) {
                values.put("created_date", now);
            }
            if (!values.containsKey(NotePad.Notes.COLUMN_NAME_TAG_COLOR)) {
                values.put(NotePad.Notes.COLUMN_NAME_TAG_COLOR, DEFAULT_TAG_COLOR);
            }
            if (!values.containsKey(NotePad.Notes.COLUMN_NAME_TAG_NAME)) {
                values.put(NotePad.Notes.COLUMN_NAME_TAG_NAME, "新标签");
            }

            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            long rowId = db.insert(NotePad.Notes.TABLE_NAME_TAGS, null, values);

            if (rowId > 0) {
                Uri tagUri = ContentUris.withAppendedId(NotePad.Notes.TAG_ID_URI_BASE, rowId);
                getContext().getContentResolver().notifyChange(tagUri, null);
                return tagUri;
            }
            throw new SQLException("插入标签失败 " + uri);
            // 验证传入的 URI。仅允许完整的提供程序 URI 进行插入。
        }else if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("未知 URI " + uri);
        }

        // 一个用于保存新记录值的映射。
        ContentValues values;

        // 如果传入的值映射不为 null，则将其用于新值。
        if (initialValues != null) {
            values = new ContentValues(initialValues);

        } else {
            // 否则，创建一个新的值映射
            values = new ContentValues();
        }

        // 获取以毫秒为单位的当前系统时间
        Long now = Long.valueOf(System.currentTimeMillis());

        // 如果值映射不包含修改日期，则将值设置为当前时间。
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
        }

        // 如果值映射不包含标题，则将值设置为默认标题。
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE) == false) {
            Resources r = Resources.getSystem();
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, r.getString(android.R.string.untitled));
        }

        // 如果值映射不包含笔记文本，则将值设置为空字符串。
        if (values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
        }

        // 以"写入"模式打开数据库对象。
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // 执行插入并返回新笔记的 ID。
        long rowId = db.insert(
                NotePad.Notes.TABLE_NAME,        // 要插入的表。
                NotePad.Notes.COLUMN_NAME_NOTE,  // 一种 hack，如果 values 为空，SQLite 将此列值设置为 null。
                values                           // 列名的映射，以及要插入列的值。
        );

        // 如果插入成功，则行 ID 存在。
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);

            // 更新Widget
            NotePadAppWidget.updateAllWidgets(getContext());

            return noteUri;
        }
        getContext().getContentResolver().notifyChange(uri, null);

// 更新Widget
        NotePadAppWidget.updateAllWidgets(getContext());

        // 如果插入未成功，则 rowID <= 0。抛出异常。
        throw new SQLException("插入行到 " + uri + " 失败");
    }

    /**
     * 当客户端调用
     * {@link android.content.ContentResolver#delete(Uri, String, String[])} 时调用此方法。
     * 从数据库删除记录。如果传入的 URI 匹配笔记 ID URI 模式，
     * 此方法删除 URI 中 ID 指定的一个记录。否则，它删除一组记录。记录还必须匹配由 where 和 whereArgs 指定的输入选择条件。
     *
     * 如果删除了行，则通知监听器更改。
     * @return 如果使用了 "where" 子句，则返回受影响的行数，否则返回 0。要删除所有行并获取行计数，请使用 "1" 作为 where 子句。
     * @throws IllegalArgumentException 如果传入的 URI 模式无效。
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        // 以"写入"模式打开数据库对象。
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;

        int count;

        // 根据传入的 URI 模式进行删除。
        switch (sUriMatcher.match(uri)) {

            // 如果传入的模式匹配 notes 的通用模式，则根据传入的 "where" 列和参数进行删除。
            case NOTES:
                count = db.delete(
                        NotePad.Notes.TABLE_NAME,  // 数据库表名
                        where,                     // 传入的 where 子句列名
                        whereArgs                  // 传入的 where 子句值
                );
                break;

            // 如果传入的 URI 匹配单个笔记 ID，则根据传入的数据进行删除，但修改 where 子句以将其限制在特定的笔记 ID。
            case NOTE_ID:
                /*
                 * 通过将其限制为所需的笔记 ID 来开始最终的 WHERE 子句。
                 */
                finalWhere =
                        NotePad.Notes._ID +                              // ID 列名
                                " = " +                                          // 相等测试
                                uri.getPathSegments().                           // 传入的笔记 ID
                                        get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                // 如果有额外的选择条件，将它们附加到最终的 WHERE 子句
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                // 执行删除。
                count = db.delete(
                        NotePad.Notes.TABLE_NAME,  // 数据库表名。
                        finalWhere,                // 最终的 WHERE 子句
                        whereArgs                  // 传入的 where 子句值。
                );
                break;

            // 如果传入的模式无效，抛出异常。
            default:
                throw new IllegalArgumentException("未知 URI " + uri);
        }

        /*获取当前上下文的内容解析器对象的句柄，并通知它传入的 URI 已更改。该对象将其传递给解析器框架，
         * 并向已为提供程序注册自己的观察者发出通知。
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // 返回删除的行数。
        return count;
    }

    /**
     * 当客户端调用
     * {@link android.content.ContentResolver#update(Uri,ContentValues,String,String[])} 时调用此方法
     * 更新数据库中的记录。值映射中的键指定的列名
     * 使用映射中的值指定的新数据进行更新。如果传入的 URI 匹配
     * 笔记 ID URI 模式，则该方法更新 URI 中 ID 指定的一个记录；
     * 否则，它更新一组记录。记录必须匹配输入
     * 由 where 和 whereArgs 指定的选择条件。
     * 如果行被更新，则通知监听器更改。
     *
     * @param uri 要匹配和更新的 URI 模式。
     * @param values 列名（键）和新值（值）的映射。
     * @param where 根据列值选择记录的 SQL "WHERE" 子句。如果为 null，则选择匹配 URI 模式的所有记录。
     * @param whereArgs 选择条件的数组。如果 "where" 参数包含值占位符 ("?")，则每个占位符由数组中的相应元素替换。
     * @return 更新的行数。
     * @throws IllegalArgumentException 如果传入的 URI 模式无效。
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        // 以"写入"模式打开数据库对象。
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        // 根据传入的 URI 模式进行更新
        switch (sUriMatcher.match(uri)) {

            // 如果传入的 URI 匹配 notes 的通用模式，则根据传入的数据进行更新。
            case NOTES:

                // 执行更新并返回更新的行数。
                count = db.update(
                        NotePad.Notes.TABLE_NAME, // 数据库表名。
                        values,                   // 列名和新值的映射。
                        where,                    // where 子句列名。
                        whereArgs                 // where 子句列值以进行选择。
                );
                break;

            // 如果传入的 URI 匹配单个笔记 ID，则根据传入的数据进行更新，但修改 where 子句以将其限制在特定的笔记 ID。
            case NOTE_ID:
                // 从传入的 URI 获取笔记 ID
                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);

                /*
                 * 通过将其限制为传入的笔记 ID 来开始创建最终的 WHERE 子句。
                 */
                finalWhere =
                        NotePad.Notes._ID +                              // ID 列名
                                " = " +                                          // 相等测试
                                uri.getPathSegments().                           // 传入的笔记 ID
                                        get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                // 如果有额外的选择条件，将它们附加到最终的 WHERE 子句
                if (where !=null) {
                    finalWhere = finalWhere + " AND " + where;
                }


                // 执行更新并返回更新的行数。
                count = db.update(
                        NotePad.Notes.TABLE_NAME, // 数据库表名。
                        values,                   // 列名和新值的映射。
                        finalWhere,               // 要使用的最终 WHERE 子句
                        // whereArgs 的占位符
                        whereArgs                 // where 子句列值以进行选择，或者
                        // 如果值在 where 参数中则为 null。
                );
                break;
            // 如果传入的模式无效，抛出异常。
            default:
                throw new IllegalArgumentException("未知 URI " + uri);
        }

        /*获取当前上下文的内容解析器对象的句柄，并通知它传入的 URI 已更改。该对象将其传递给解析器框架，
         * 并向已为提供程序注册自己的观察者发出通知。
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // 返回更新的行数。
        return count;
    }

    /**
     * 测试包可以调用此方法来获取 NotePadProvider 底层数据库的句柄，
     * 以便它可以向数据库插入测试数据。测试用例类负责
     * 在测试上下文中实例化提供程序；{@link android.test.ProviderTestCase2} 在 setUp() 调用期间执行此操作
     *
     * @return 提供程序数据的数据库辅助对象的句柄。
     */
    DatabaseHelper getOpenHelperForTest() {
        return mOpenHelper;
    }
}
