package com.cutter72.ultrasonicsensor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FileManager {

    public File prepareDirectory(String pathToFile) {
        File filePath = new File(pathToFile);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        return filePath;
    }

    public void writeToFile(File file, String data) {
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
