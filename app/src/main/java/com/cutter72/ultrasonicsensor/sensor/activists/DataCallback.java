package com.cutter72.ultrasonicsensor.sensor.activists;

import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;

public interface DataCallback {
    void onDataReceive(SensorDataCarrier data);
}
