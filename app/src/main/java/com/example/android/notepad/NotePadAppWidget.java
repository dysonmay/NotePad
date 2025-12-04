package com.example.android.notepad;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotePadAppWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // 当第一个Widget被添加到桌面时调用
        Toast.makeText(context, "笔记小组件已启用", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context) {
        // 当最后一个Widget被从桌面移除时调用
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_note_pad);

        // 1. 获取笔记统计数据
        int noteCount = getNoteCount(context);
        // 2. 获取最新的笔记内容
        String latestNoteContent = getLatestNoteContent(context); // 使用新方法

        // 3. 更新Widget显示
        views.setTextViewText(R.id.widget_note_count, "笔记: " + noteCount);
        // 将内容设置到新的TextView上，例如 R.id.widget_note_content
        views.setTextViewText(R.id.widget_note_content, latestNoteContent);

        // 设置点击事件：打开应用主界面
        Intent mainIntent = new Intent(context, NotesList.class);
        mainIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_icon, mainPendingIntent);

        // 设置点击事件：打开新建笔记
        Intent newNoteIntent = new Intent(context, NoteEditor.class);
        newNoteIntent.setAction(Intent.ACTION_INSERT);
        newNoteIntent.setData(NotePad.Notes.CONTENT_URI);
        PendingIntent newNotePendingIntent = PendingIntent.getActivity(
                context, 1, newNoteIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_new_note_btn, newNotePendingIntent);

        // 设置点击Widget整体打开应用
        views.setOnClickPendingIntent(R.id.widget_title, mainPendingIntent);

        // 告诉AppWidgetManager对当前Widget执行更新
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static int getNoteCount(Context context) {
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    NotePad.Notes.CONTENT_URI,
                    new String[]{"COUNT(*) AS count"},
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    private static String getLatestNoteContent(Context context) {
        String latestNoteContent = "[暂无内容]"; // 默认文本
        Cursor cursor = null;
        try {
            // 查询最新的一条笔记，按修改时间降序排列
            cursor = context.getContentResolver().query(
                    NotePad.Notes.CONTENT_URI,
                    new String[]{
                            NotePad.Notes.COLUMN_NAME_NOTE, // 重点：查询笔记内容列
                            NotePad.Notes.COLUMN_NAME_TITLE
                    },
                    null,
                    null,
                    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC LIMIT 1" // 取最新一条
            );

            if (cursor != null && cursor.moveToFirst()) {
                // 获取笔记内容
                String content = cursor.getString(0);
                String title = cursor.getString(1);

                // 处理内容：如果为空，则用标题或默认文本
                if (content != null && !content.trim().isEmpty()) {
                    latestNoteContent = content.trim();
                } else if (title != null && !title.trim().isEmpty()) {
                    latestNoteContent = "[无正文] " + title.trim();
                }

                // 对过长的内容进行截断，避免小组件显示异常
                int maxLength = 100; // 可根据你的小组件布局调整最大长度
                if (latestNoteContent.length() > maxLength) {
                    latestNoteContent = latestNoteContent.substring(0, maxLength) + "...";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            latestNoteContent = "加载失败";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return latestNoteContent;
    }
    // 提供静态方法供其他组件手动更新Widget
    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, NotePadAppWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        if (appWidgetIds.length > 0) {
            new NotePadAppWidget().onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}