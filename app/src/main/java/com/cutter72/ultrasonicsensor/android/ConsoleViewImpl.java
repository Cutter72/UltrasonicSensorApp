package com.cutter72.ultrasonicsensor.android;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

@SuppressWarnings({"Convert2Lambda", "FieldCanBeLocal"})
public class ConsoleViewImpl implements ConsoleView {
    private final int CONSOLE_LINE_CHARS_LIMIT = 99999;
    private final int CONSOLE_LINES_LIMIT = 999;
    private final LinearLayout linearLayout;
    private final ScrollView scrollView;
    private TextView previousLine;

    public ConsoleViewImpl(LinearLayout linearLayout, ScrollView scrollView) {
        this.linearLayout = linearLayout;
        this.scrollView = scrollView;
        this.linearLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void print(@Nullable Object object) {
        printf("%s", object);
    }

    @Override
    public void println() {
        printf("%n");
    }

    @Override
    public void println(@Nullable Object object) {
        printf("%s%n", object);
    }

    @Override
    public void printf(@NonNull String s, Object... args) {
        String text = String.format(Locale.getDefault(), s, args);
        clearIfFull();
        boolean isNewLine = false;
        if (previousLine == null) {
            previousLine = createLineView();
            isNewLine = true;
        }
        String textToSet = previousLine.getText().toString() + text;
        previousLine.setText(textToSet);
        if (isNewLine) {
            linearLayout.addView(previousLine);
        }
    }

    private TextView createLineView() {
        TextView lineView = new TextView(linearLayout.getContext());
        lineView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return lineView;
    }

    private void clearIfFull() {
        if (isFull()) {
            clear();
        }
    }

    private boolean isFull() {
        if (previousLine != null) {
            return getSize() > CONSOLE_LINES_LIMIT || previousLine.getText().length() > CONSOLE_LINE_CHARS_LIMIT;
        } else {
            return getSize() > CONSOLE_LINES_LIMIT;
        }
    }

    public void clear() {
        linearLayout.removeAllViews();
        previousLine = null;
    }

    public int getSize() {
        return linearLayout.getChildCount();
    }
}
