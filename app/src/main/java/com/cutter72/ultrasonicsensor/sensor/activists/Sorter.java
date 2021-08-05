package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.Collections;
import java.util.List;

public interface Sorter {

    default void sortByDistance(@NonNull List<Measurement> measurementsToSort) {
        Collections.sort(measurementsToSort, (o1, o2) ->
                Double.compare(o1.getDistanceCentimeters(), o2.getDistanceCentimeters()));
    }

    default void sortById(@NonNull List<Measurement> measurementsToSort) {
        Collections.sort(measurementsToSort, (o1, o2) -> Integer.compare(o1.getId(), o2.getId()));
    }
}
