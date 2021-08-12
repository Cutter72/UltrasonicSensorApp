package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;

public interface DataFilter {
    @NonNull
    SensorDataCarrier filterByMedian(@NonNull SensorDataCarrier dataToFilter, double maxDeviationFromMedianInCentimeters);
}
