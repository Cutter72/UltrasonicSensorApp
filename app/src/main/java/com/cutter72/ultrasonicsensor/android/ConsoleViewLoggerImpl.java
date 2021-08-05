package com.cutter72.ultrasonicsensor.android;


import android.util.Log;

import androidx.annotation.NonNull;

import com.balsikandar.crashreporter.CrashReporter;

public class ConsoleViewLoggerImpl implements ConsoleViewLogger {
    private static ConsoleViewLoggerImpl instance;
    private ConsoleViewImpl consoleView;

    public ConsoleViewLoggerImpl(ConsoleViewImpl consoleView) {
        this.consoleView = consoleView;
    }

    public static synchronized ConsoleViewLoggerImpl getInstance() {
        if (instance == null) {
            throw new RuntimeException("Logger not initialized! Use LogWrapper#initializeLogger(@NonNull ConsoleViewImpl consoleView) first!");
        }
        return instance;
    }

    public static synchronized ConsoleViewLoggerImpl initializeLogger(@NonNull ConsoleViewImpl consoleView) {
        if (instance == null) {
            instance = new ConsoleViewLoggerImpl(consoleView);
        }
        return instance;
    }

    @Override
    public void logException(String tag, Exception e) {
        e.printStackTrace();
        Log.e(tag, e.getMessage(), e);
        CrashReporter.logException(e);
        consoleView.println(Log.getStackTraceString(e));
    }

    @Override
    public void v(String tag, String msg) {
        Log.v(tag, msg);
        consoleView.println(msg);
    }

    @Override
    public void d(String tag, String msg) {
        Log.d(tag, msg);
        consoleView.println(msg);
    }

    @Override
    public void i(String tag, String msg) {
        Log.i(tag, msg);
        consoleView.println(msg);
    }

    @Override
    public void w(String tag, String msg) {
        Log.w(tag, msg);
        consoleView.println(msg);
    }

    @Override
    public void e(String tag, String msg) {
        Log.e(tag, msg);
        consoleView.println(msg);
    }
}