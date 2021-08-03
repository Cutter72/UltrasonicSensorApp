package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public interface MeasurementsManager {
    @NonNull
    List<Measurement> filterByMedian(@NonNull List<Measurement> measurementsToFilter, double maxDeviationFromMedianInCentimeters);

    void sortByDistance(@NonNull List<Measurement> measurementsToSort);

    void sortById(@NonNull List<Measurement> measurementsToSort);
}
