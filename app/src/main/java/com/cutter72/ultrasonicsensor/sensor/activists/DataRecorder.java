package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.DataStorage;

public interface DataRecorder {
    boolean startRecording();

    boolean stopRecording();

    boolean isRecording();

    @NonNull
    DataStorage getRecordedData();
}
