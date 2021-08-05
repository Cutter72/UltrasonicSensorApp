package com.cutter72.ultrasonicsensor.android.other;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface ConsoleView {
    void print(@Nullable Object object);

    void println();

    void println(@Nullable Object object);

    void printf(@NonNull String s, Object... args);
}
