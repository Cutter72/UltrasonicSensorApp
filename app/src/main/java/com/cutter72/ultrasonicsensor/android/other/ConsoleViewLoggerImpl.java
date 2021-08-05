package com.cutter72.ultrasonicsensor.android.other;


import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.balsikandar.crashreporter.CrashReporter;

public class ConsoleViewLoggerImpl implements ConsoleViewLogger {
    private static ConsoleViewLogger instance;
    private Activity activity;
    private ConsoleView consoleView;

    public ConsoleViewLoggerImpl(@NonNull Activity activity, @NonNull ConsoleView consoleView) {
        this.activity = activity;
        this.consoleView = consoleView;
    }

    public static synchronized ConsoleViewLogger getInstance() {
        if (instance == null) {
            throw new RuntimeException("Logger not initialized! Use method " +
                    "LogWrapper#initializeLogger(@NonNull ConsoleViewImpl consoleView) first!");
        }
        return instance;
    }

    public static synchronized ConsoleViewLogger initializeLogger(@NonNull Activity activity, @NonNull ConsoleView consoleView) {
        if (instance == null) {
            instance = new ConsoleViewLoggerImpl(activity, consoleView);
        }
        return instance;
    }

    @Override
    public void logException(String tag, Exception e) {
        e.printStackTrace();
        Log.e(tag, e.getMessage(), e);
        CrashReporter.logException(e);
        activity.runOnUiThread(() -> consoleView.println(Log.getStackTraceString(e)));
    }

    @Override
    public void v(String tag, String msg) {
        Log.v(tag, msg);
    }

    @Override
    public void d(String tag, String msg) {
        Log.d(tag, msg);
        activity.runOnUiThread(() -> consoleView.println(msg));
    }

    @Override
    public void i(String tag, String msg) {
        Log.i(tag, msg);
        activity.runOnUiThread(() -> consoleView.println(msg));
    }

    @Override
    public void w(String tag, String msg) {
        Log.w(tag, msg);
        activity.runOnUiThread(() -> consoleView.println(msg));
    }

    @Override
    public void e(String tag, String msg) {
        Log.e(tag, msg);
        activity.runOnUiThread(() -> consoleView.println(msg));
    }
}
