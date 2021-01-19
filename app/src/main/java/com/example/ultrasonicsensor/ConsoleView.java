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
    private TextView previousLine;

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

    public void println() {
        Context context = linearLayout.getContext();
        previousLine = new TextView(context);
        previousLine.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        previousLine.setText("");
        linearLayout.addView(previousLine);
    }

    public void println(Object object) {
        String text = object.toString();
        System.out.println(text);
        Context context = linearLayout.getContext();
        previousLine = new TextView(context);
        previousLine.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        previousLine.setText(text);
        linearLayout.addView(previousLine);
    }

    public void print(Object object) {
        boolean isNewLine = false;
        String text = object.toString();
        System.out.print(text);
        Context context = linearLayout.getContext();
        if (previousLine == null) {
            previousLine = new TextView(context);
            previousLine.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            isNewLine = true;
        }
        String textToSet = previousLine.getText().toString() + text;
        previousLine.setText(textToSet);
        if (isNewLine) {
            linearLayout.addView(previousLine);
        }
    }

    public void clear() {
        linearLayout.removeAllViews();
    }
}
