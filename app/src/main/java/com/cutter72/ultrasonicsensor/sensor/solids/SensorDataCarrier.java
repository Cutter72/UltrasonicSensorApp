package com.cutter72.ultrasonicsensor.sensor.solids;

import androidx.annotation.NonNull;

import java.util.List;

public interface SensorDataCarrier {
    SensorDataCarrier addRawData(@NonNull byte[] rawData);

    SensorDataCarrier addData(SensorDataCarrier sensorDataCarrier);

    @NonNull
    byte[] getRawData();

    @NonNull
    List<Measurement> getRawMeasurements();

    @NonNull
    List<Measurement> getLastMeasurements(int howMany);

    int size();

    Measurement get(int index);

    void clear();

    SensorDataCarrier setRawData(List<byte[]> rawMeasurements);

    SensorDataCarrier setRawData(byte[] rawData);

    SensorDataCarrier setRawMeasurements(List<Measurement> measurements);
}
