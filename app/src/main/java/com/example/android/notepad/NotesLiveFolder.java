/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.provider.LiveFolders;

/**
 * 此 Activity 创建一个 live folder Intent 并将其发送回 HOME。
 * 根据 Intent 中的数据，HOME 创建一个 live folder 并在 Home 视图中显示其图标。
 * 当用户点击该图标时，Home 使用从 Intent 获取的数据从内容提供程序检索信息并在视图中显示。
 *
 * 此 Activity 的意图过滤器设置为 ACTION_CREATE_LIVE_FOLDER，
 * 这是 HOME 在长按并选择 Live Folder 时发送的。
 */
public class NotesLiveFolder extends Activity {

    /**
     * 所有工作都在 onCreate() 中完成。此 Activity 实际上不显示 UI。
     * 相反，它设置一个 Intent 并将其返回给调用者（HOME 活动）。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * 获取传入的 Intent 及其操作。如果传入的 Intent 是
         * ACTION_CREATE_LIVE_FOLDER，则创建一个包含必要数据的传出 Intent
         * 并返回 OK。否则，返回 CANCEL。
         */
        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action)) {

            // 创建一个新的 Intent。
            final Intent liveFolderIntent = new Intent();

            /*
             * 以下语句将数据放入传出的 Intent 中。请参阅
             * {@link android.provider.LiveFolders 了解这些数据值的详细描述。
             * 根据这些数据，HOME 设置一个 live folder。
             */
            // 设置支持文件夹的内容提供程序的 URI 模式。
            liveFolderIntent.setData(NotePad.Notes.LIVE_FOLDER_URI);

            // 将 live folder 的显示名称作为 Extra 字符串添加。
            String foldername = getString(R.string.live_folder_name);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME, foldername);

            // 将 live folder 的显示图标作为 Extra 资源添加。
            ShortcutIconResource foldericon =
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.live_folder_notes);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON, foldericon);

            // 将 live folder 的显示模式作为整数添加。指定的
            // 模式使 live folder 以列表形式显示。
            liveFolderIntent.putExtra(
                    LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
                    LiveFolders.DISPLAY_MODE_LIST);

            /*
             * 为 live folder 列表中的项目添加基本操作，作为 Intent。当
             * 用户点击列表中的单个笔记时，live folder 会触发此 Intent。
             *
             * 其操作是 ACTION_EDIT，因此它会触发 Note Editor 活动。
             * 其数据是用于通过 ID 标识单个笔记的 URI 模式。live folder
             * 会自动将所选项目的 ID 值添加到 URI 模式。
             *
             * 结果，Note Editor 被触发并获取一个要通过 ID 检索的单个笔记。
             */
            Intent returnIntent
                    = new Intent(Intent.ACTION_EDIT, NotePad.Notes.CONTENT_ID_URI_PATTERN);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT, returnIntent);

            /* 创建一个 ActivityResult 对象以传播回 HOME。将其结果指示器
             * 设置为 OK，并将返回的 Intent 设置为刚刚构建的 live folder Intent。
             */
            setResult(RESULT_OK, liveFolderIntent);

        } else {

            // 如果原始操作不是 ACTION_CREATE_LIVE_FOLDER，则创建一个
            // 结果指示器设置为 CANCELED 的 ActivityResult，但不返回 Intent
            setResult(RESULT_CANCELED);
        }

        // 关闭 Activity。ActivityObject 被传播回调用者。
        finish();
    }
}