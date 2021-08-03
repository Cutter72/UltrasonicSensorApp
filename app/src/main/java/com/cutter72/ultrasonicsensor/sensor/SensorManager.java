package com.cutter72.ultrasonicsensor.sensor;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public interface SensorManager {
    boolean openConnectionToSensor();

    boolean isSensorConnectionOpen();

    List<Measurement> readMeasurementsFromSensor();
}
