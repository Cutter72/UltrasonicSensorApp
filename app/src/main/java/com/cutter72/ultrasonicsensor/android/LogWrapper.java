package com.cutter72.ultrasonicsensor.android;


import android.util.Log;

import androidx.annotation.NonNull;

import com.balsikandar.crashreporter.CrashReporter;

public class LogWrapper {
    private static LogWrapper instance;
    private ConsoleViewImpl consoleView;

    public LogWrapper(ConsoleViewImpl consoleView) {
        this.consoleView = consoleView;
    }

    public static synchronized LogWrapper getInstance() {
        if (instance == null) {
            throw new RuntimeException("Logger not initialized! Use LogWrapper#initializeLogger(@NonNull ConsoleViewImpl consoleView) first!");
        }
        return instance;
    }

    public static synchronized LogWrapper initializeLogger(@NonNull ConsoleViewImpl consoleView) {
        if (instance == null) {
            instance = new LogWrapper(consoleView);
        }
        return instance;
    }

    public void logException(String tag, Exception e) {
        e.printStackTrace();
        Log.e(tag, e.getMessage(), e);
        CrashReporter.logException(e);
        consoleView.println(Log.getStackTraceString(e));
    }

    public void v(String tag, String msg) {
        Log.v(tag, msg);
        consoleView.println(msg);
    }

    public void d(String tag, String msg) {
        Log.d(tag, msg);
        consoleView.println(msg);
    }

    public void i(String tag, String msg) {
        Log.i(tag, msg);
        consoleView.println(msg);
    }

    public void w(String tag, String msg) {
        Log.w(tag, msg);
        consoleView.println(msg);
    }

    public void e(String tag, String msg) {
        Log.e(tag, msg);
        consoleView.println(msg);
    }
}
