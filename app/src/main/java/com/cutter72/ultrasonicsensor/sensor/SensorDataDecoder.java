package com.cutter72.ultrasonicsensor.sensor;

import java.util.List;

public interface SensorDataDecoder {
    List<Measurement> decodeDataFromSensor(byte[] rawDataFromSensor);
}
