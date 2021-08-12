package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public interface DataDecoder {
    @NonNull
    List<Measurement> decodeDataFromSensor(@NonNull byte[] rawDataFromSensor);
}
