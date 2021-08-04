package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.DataStorage;

public interface DataRecorder {
    void startRecording();

    void stopRecording();

    boolean isRecording();

    @NonNull
    DataStorage getRecordedData();
}
