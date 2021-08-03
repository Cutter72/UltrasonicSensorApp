package com.cutter72.ultrasonicsensor.sensor.activists;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public interface DataDecoder {
    List<Measurement> decodeDataFromSensor(byte[] rawDataFromSensor);
}
