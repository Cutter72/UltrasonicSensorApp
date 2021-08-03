package com.cutter72.ultrasonicsensor.files;

import androidx.annotation.NonNull;

import java.io.File;

public interface FilesManager {
    File prepareDirectory(@NonNull String pathToFile);

    void writeToFile(@NonNull File file, @NonNull String data);
}
