package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.SensorConnection;
import com.cutter72.ultrasonicsensor.sensor.solids.DataStorage;

public class DataRecorderImpl implements DataRecorder {
    private final DataStorage dataStorage;
    private final SensorConnection sensorConnection;

    public DataRecorderImpl(@NonNull SensorConnection sensorConnection, @NonNull DataStorage dataStorage) {
        this.dataStorage = dataStorage;
        this.sensorConnection = sensorConnection;
    }

    @Override
    public boolean startRecording() {
        return false;
    }

    @Override
    public boolean stopRecording() {
        return false;
    }

    @Override
    public boolean isRecording() {
        return false;
    }

    @NonNull
    @Override
    public DataStorage getRecordedData() {
        return this.dataStorage;
    }
}
