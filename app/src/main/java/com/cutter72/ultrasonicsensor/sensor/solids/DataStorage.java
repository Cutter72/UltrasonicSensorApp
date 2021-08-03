package com.cutter72.ultrasonicsensor.sensor.solids;

import androidx.annotation.NonNull;

import java.util.List;

public interface DataStorage {
    void addRawData(@NonNull byte[] rawData);

    @NonNull
    byte[] getRawData();

    @NonNull
    List<Measurement> getRawMeasurements();

    @NonNull
    List<Measurement> getLastMeasurements(int howMany);
}
