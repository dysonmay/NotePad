package com.example.android.notepad;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

import java.util.List;

public class ColorGridAdapter extends BaseAdapter {
    private Context context;
    private int[] colors;

    public ColorGridAdapter(Context context, int[] colors) {
        this.context = context;
        this.colors = colors;
    }

    @Override
    public int getCount() {
        return colors.length;
    }

    @Override
    public Object getItem(int position) {
        return colors[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new View(context);
            int size = (int) (context.getResources().getDisplayMetrics().density * 48);
            convertView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        }

        convertView.setBackgroundColor(colors[position]);
        convertView.setTag(colors[position]);

        return convertView;
    }
}