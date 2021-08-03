package com.cutter72.ultrasonicsensor.sensor.activists;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public interface Measurements {
    List<Measurement> filterByMedian(List<Measurement> measurementsToFilter, double maxDeviationFromMedianInCentimeters);

    List<Measurement> sortByDistance(List<Measurement> measurementsToSort);

    List<Measurement> sortById(List<Measurement> measurementsToSort);
}
