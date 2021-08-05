package com.cutter72.ultrasonicsensor.android;

public interface ConsoleViewLogger {
    void logException(String tag, Exception e);

    void v(String tag, String msg);

    void d(String tag, String msg);

    void i(String tag, String msg);

    void w(String tag, String msg);

    void e(String tag, String msg);
}
