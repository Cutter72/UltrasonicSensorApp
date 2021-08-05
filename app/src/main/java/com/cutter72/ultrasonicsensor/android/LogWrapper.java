package com.cutter72.ultrasonicsensor.android;


import android.util.Log;

import com.balsikandar.crashreporter.CrashReporter;

public class LogWrapper {
    private static LogWrapper instance;
    private ConsoleView consoleView;

    public LogWrapper(ConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public static synchronized LogWrapper getInstance(ConsoleView consoleView) {
        if (instance == null) {
            instance = new LogWrapper(consoleView);
        }
        return instance;
    }

    public void logException(String tag, Exception e) {
        Log.e(tag, e.getMessage(), e);
        CrashReporter.logException(e);
        consoleView.printf("%s%d%n");
        System.out.printf("");
    }

    public void i(String tag, String msg) {
        Log.i(tag, msg);
    }
}
