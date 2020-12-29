package com.example.ultrasonicsensor;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

@SuppressWarnings("Convert2Lambda")
public class ConsoleView {
    private LinearLayout linearLayout;
    private ScrollView scrollView;

    public ConsoleView(LinearLayout linearLayout, ScrollView scrollView) {
        this.linearLayout = linearLayout;
        this.scrollView = scrollView;
        this.linearLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public void println(Object object) {
        String text = object.toString();
        System.out.println(text);
        Context context = linearLayout.getContext();
        TextView newLine = new TextView(context);
        newLine.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        newLine.setText(text);
        linearLayout.addView(newLine);
    }
}
