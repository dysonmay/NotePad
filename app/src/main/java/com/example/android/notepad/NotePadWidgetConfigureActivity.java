package com.example.android.notepad;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class NotePadWidgetConfigureActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "com.example.android.notepad.NotePadAppWidget";
    private static final String PREF_PREFIX_KEY = "widget_";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // 设置结果默认为取消
        setResult(RESULT_CANCELED);

        setContentView(R.layout.widget_configure);

        // 找到保存按钮
        findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Context context = NotePadWidgetConfigureActivity.this;

                // 保存配置
                saveWidgetPref(context, mAppWidgetId, "default");

                // 更新Widget
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                NotePadAppWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

                // 设置结果并关闭
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });

        // 找到取消按钮
        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        // 获取Widget ID
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // 如果没有有效的Widget ID，直接退出
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
    }

    // 保存Widget配置到SharedPreferences
    static void saveWidgetPref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.apply();
    }

    // 读取Widget配置
    static String loadWidgetPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String title = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (title != null) {
            return title;
        } else {
            return context.getString(R.string.app_name);
        }
    }

    // 删除Widget配置
    static void deleteWidgetPref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }
}
