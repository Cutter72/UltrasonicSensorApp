package com.cutter72.ultrasonicsensor.android.other;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

@SuppressWarnings({"FieldCanBeLocal", "Convert2Lambda"})
public class ConsoleViewImpl implements ConsoleView {
    private final int CONSOLE_LINE_CHARS_LIMIT = 43210;
    private final LinearLayout linearLayout;
    private final TextView consoleLineView;

    public ConsoleViewImpl(@NonNull LinearLayout linearLayout, @NonNull ScrollView scrollView) {
        this.consoleLineView = createLineView(linearLayout.getContext());
        this.linearLayout = linearLayout;
        this.linearLayout.removeAllViews();
        this.linearLayout.addView(consoleLineView);
        this.linearLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
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
        String textToSet = consoleLineView.getText().toString() + text;
        consoleLineView.setText(textToSet);
    }

    private TextView createLineView(Context context) {
        TextView lineView = new TextView(context);
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
        return consoleLineView.getText().length() > CONSOLE_LINE_CHARS_LIMIT;
    }

    @Override
    public void clear() {
        consoleLineView.setText("CONSOLE CLEARED\n");
    }
}
