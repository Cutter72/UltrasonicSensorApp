package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public interface ImpactCounter {

    int findImpacts(@NonNull List<Measurement> measurementsToSearch,
                    @IntRange(from = 5, to = 20) int measurementWindow,
                    @IntRange(from = 50, to = 2000) long minTimeIntervalBetweenImpactMillis,
                    @FloatRange(from = 0.1, to = 2.0) double minDifference);
}
