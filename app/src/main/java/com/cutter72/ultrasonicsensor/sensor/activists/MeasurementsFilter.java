package com.cutter72.ultrasonicsensor.sensor.activists;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public interface MeasurementsFilter {
    List<Measurement> filterByMedian(List<Measurement> measurementsToFilter, double maxDeviationFromMedianInCentimeters);
}
