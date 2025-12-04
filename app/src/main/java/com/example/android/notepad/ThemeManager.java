package com.example.android.notepad;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

/**
 * 主题管理工具类
 */
public class ThemeManager {

    private static final String PREF_THEME_INDEX = "theme_index";
    private static final int DEFAULT_THEME_INDEX = 0;

    // 主题颜色数组（主色，深色，强调色）
    private static final int[][] THEME_COLORS = {
            {0xFF2196F3, 0xFF1976D2, 0xFFFF4081}, // 蓝色主题
            {0xFF4CAF50, 0xFF388E3C, 0xFF8BC34A}, // 绿色主题
            {0xFFFF5722, 0xFFD84315, 0xFFFF9800}, // 橙色主题
            {0xFF9C27B0, 0xFF7B1FA2, 0xFFE040FB}, // 紫色主题
            {0xFF00BCD4, 0xFF0097A7, 0xFF18FFFF}, // 青色主题
            {0xFFFF9800, 0xFFF57C00, 0xFFFFD740}  // 琥珀色主题
    };

    /**
     * 获取当前主题索引
     */
    public static int getCurrentThemeIndex(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(PREF_THEME_INDEX, DEFAULT_THEME_INDEX);
    }

    /**
     * 设置主题索引
     */
    public static void setThemeIndex(Context context, int index) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(PREF_THEME_INDEX, index).apply();
    }

    /**
     * 获取主题颜色
     */
    public static int getThemeColor(Context context, int colorType) {
        int index = getCurrentThemeIndex(context);
        if (index >= 0 && index < THEME_COLORS.length) {
            return THEME_COLORS[index][colorType];
        }
        return THEME_COLORS[DEFAULT_THEME_INDEX][colorType];
    }

    /**
     * 获取主题颜色数组
     */
    public static int[][] getThemeColors() {
        return THEME_COLORS;
    }

    /**
     * 应用主题到Activity
     */
    public static void applyTheme(AppCompatActivity activity) {
        int themeIndex = getCurrentThemeIndex(activity);

        // 获取颜色
        int colorPrimary = THEME_COLORS[themeIndex][0];
        int colorPrimaryDark = THEME_COLORS[themeIndex][1];
        int colorAccent = THEME_COLORS[themeIndex][2];

        // 设置状态栏颜色（Android 5.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(colorPrimaryDark);
        }

        // 设置ActionBar/Toolbar颜色
        if (activity.getSupportActionBar() != null) {
            // 重新设置ActionBar的背景
            activity.getSupportActionBar().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(colorPrimary));
        }
    }

    /**
     * 获取主题名称
     */
    public static String getThemeName(Context context, int index) {
        String[] themeNames = context.getResources().getStringArray(R.array.theme_names);
        if (index >= 0 && index < themeNames.length) {
            return themeNames[index];
        }
        return themeNames[DEFAULT_THEME_INDEX];
    }
}