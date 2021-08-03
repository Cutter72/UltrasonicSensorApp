package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.SensorConnection;
import com.cutter72.ultrasonicsensor.sensor.solids.DataStorage;

public interface DataRecorder {
    void startRecording(@NonNull SensorConnection sensorConnection);

    void stopRecording();

    boolean isRecording();

    boolean isDataRecorded();

    @NonNull
    DataStorage getRecordedData();
}
