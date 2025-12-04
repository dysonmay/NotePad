/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import android.net.Uri;
import android.provider.BaseColumns;

/*
 * 定义Note Pad内容提供者与其客户端之间的契约。契约定义了
 * 客户端访问提供程序时需要的一个或多个数据表的信息。一份合同
 * 是一个公共的、不可扩展的（final）类，它包含定义列名和
 * URI。编写良好的客户端只依赖于契约中的常量。
 */
public final class NotePad {
    public static final String AUTHORITY = "com.example.android.notepad.provider";

    // 这个类不能被实例化
    private NotePad() {
    }

    /**
     * Notes表契约
     */
    public static final class Notes implements BaseColumns {

        // 这个类不能被实例化
        private Notes() {}

        /**
         * 此提供程序提供的表名
         */
        public static final String TABLE_NAME = "notes";

        /*
         * URI定义
         */

        /**
         * 此提供程序URI的方案部分
         */
        private static final String SCHEME = "content://";

        /**
         * URI的路径部分
         */

        /**
         * Notes URI的路径部分
         */
        private static final String PATH_NOTES = "/notes";

        /**
         * 笔记ID URI的路径部分
         */
        private static final String PATH_NOTE_ID = "/notes/";

        /**
         * 在笔记ID URI的路径部分中，笔记ID段的相对位置（从0开始）
         */
        public static final int NOTE_ID_PATH_POSITION = 1;

        /**
         * 实时文件夹URI的路径部分
         */
        private static final String PATH_LIVE_FOLDER = "/live_folders/notes";

        /**
         * 此表的内容URI样式URL
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_NOTES);

        /**
         * 单个笔记的内容URI基础。调用者必须
         * 在此Uri后附加数字笔记ID以检索笔记
         */
        public static final Uri CONTENT_ID_URI_BASE
                = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID);

        /**
         * 由ID指定的单个笔记的内容URI匹配模式。使用此模式来匹配
         * 传入的URI或构造Intent。
         */
        public static final Uri CONTENT_ID_URI_PATTERN
                = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID + "/#");

        /**
         * 用于实时文件夹的笔记列表的内容Uri模式
         */
        public static final Uri LIVE_FOLDER_URI
                = Uri.parse(SCHEME + AUTHORITY + PATH_LIVE_FOLDER);

        /*
         * MIME类型定义
         */

        /**
         * 提供笔记目录的 {@link #CONTENT_URI} 的MIME类型。
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.note";

        /**
         * 单个笔记的 {@link #CONTENT_URI} 子目录的MIME类型。
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.note";

        /**
         * 此表的默认排序顺序
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /*
         * 列定义
         */

        /**
         * 笔记标题的列名
         * <P>类型: TEXT</P>
         */
        public static final String COLUMN_NAME_TITLE = "title";

        /**
         * 笔记内容的列名
         * <P>类型: TEXT</P>
         */
        public static final String COLUMN_NAME_NOTE = "note";
//        /**
//         * 创建时间戳的列名
//         * <P>类型: INTEGER (来自 System.curentTimeMillis() 的长整型)</P>
//         */
//        public static final String COLUMN_NAME_CREATE_DATE = "created";

        /**
         * 修改时间戳的列名
         * <P>类型: INTEGER (来自 System.curentTimeMillis() 的长整型)</P>
         */
        public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";


        /**
         * 标签ID的列名
         * <P>类型: INTEGER</P>
         */
        public static final String COLUMN_NAME_TAG_ID = "tag_id";

        /**
         * 标签表的表名
         */
        public static final String TABLE_NAME_TAGS = "tags";

        /**
         * 标签名称的列名
         */
        public static final String COLUMN_NAME_TAG_NAME = "name";

        /**
         * 标签颜色的列名
         */
        public static final String COLUMN_NAME_TAG_COLOR = "color";

        // 标签URI定义
        private static final String PATH_TAGS = "/tags";

        // 标签MIME类型
        public static final String TAGS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.tag";
        public static final String TAG_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.tag";


        public static final Uri TAGS_CONTENT_URI = Uri.parse(
                "content://" + AUTHORITY + "/tags");
        public static final Uri TAG_ID_URI_BASE = Uri.parse(
                "content://" + AUTHORITY + "/tags/");
        public static final Uri TAG_ID_URI_PATTERN = Uri.parse(
                "content://" + AUTHORITY + "/tags/#");

        // 标签 MIME 类型
        public static final String CONTENT_TAG_TYPE = "vnd.android.cursor.dir/vnd.google.note.tag";
        public static final String CONTENT_TAG_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.note.tag";
    }
}