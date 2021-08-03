package com.cutter72.ultrasonicsensor.files;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FilesManagerImpl implements FilesManager {

    @Override
    public File prepareDirectory(@NonNull String pathToFile) {
        File filePath = new File(pathToFile);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        return filePath;
    }

    @Override
    public void writeToFile(@NonNull File file, @NonNull String data) {
        try {
            FileWriter fileWriter = new FileWriter(file);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(data);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
