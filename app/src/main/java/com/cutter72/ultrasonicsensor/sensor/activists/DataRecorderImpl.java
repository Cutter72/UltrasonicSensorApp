package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.balsikandar.crashreporter.CrashReporter;
import com.cutter72.ultrasonicsensor.sensor.SensorConnection;
import com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl;
import com.cutter72.ultrasonicsensor.sensor.solids.DataStorage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DataRecorderImpl implements DataRecorder {
    private final DataStorage dataStorage;
    private final SensorConnection sensorConnection;
    private final ExecutorService executorService;
    private boolean isRecording;

    public DataRecorderImpl(@NonNull SensorConnection sensorConnection,
                            @NonNull DataStorage dataStorage) {
        this.dataStorage = dataStorage;
        this.sensorConnection = sensorConnection;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void startRecording() {
        isRecording = true;
        executorService.submit(() -> {
            while (isRecording) {
                if (waitForData()) {
                    dataStorage.addRawData(sensorConnection.readRawData());
                } else {
                    stopRecording();
                }
            }
        });
    }

    private boolean waitForData() {
        try {
            TimeUnit.MILLISECONDS.sleep(SensorConnectionImpl.DEFAULT_BUFFER_TIME_OUT_MILLIS);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
            return false;
        }
    }

    @Override
    public void stopRecording() {
        isRecording = false;
        if (!executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    @Override
    public boolean isRecording() {
        return isRecording;
    }

    @NonNull
    @Override
    public DataStorage getRecordedData() {
        return this.dataStorage;
    }
}
