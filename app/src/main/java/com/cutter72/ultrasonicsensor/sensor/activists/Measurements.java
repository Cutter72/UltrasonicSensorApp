package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public interface Measurements {
    @NonNull
    List<Measurement> filterByMedian(@NonNull List<Measurement> measurementsToFilter, double maxDeviationFromMedianInCentimeters);

    @NonNull
    List<Measurement> sortByDistance(@NonNull List<Measurement> measurementsToSort);

    @NonNull
    List<Measurement> sortById(@NonNull List<Measurement> measurementsToSort);
}
