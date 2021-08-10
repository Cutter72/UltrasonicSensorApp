package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public interface MeasurementsTimeApproximator {

    /**
     * Changes {@link Measurement}{@code #time} of each given measurements to approximated time
     * calculated by this method. {@param timeSpan} is time span between {@code long initialTime}
     * and the {@code long finalTime}. This time span is linearly distributed between all measurements
     * starting from first and ending at last measurement. That means that first measurement time field
     * {@link Measurement}{@code #time} is unchanged and it is a timestamp {@code base} for subsequent measurements.
     * Every measurement after first has {@link Measurement}{@code #time} set to {@code new Date(base + i * timeDelta)}
     * where {@code i} is subsequent measurement number and {@code timeDelta} is a time span divided
     * by number of measurements - 1. {@code timeDelta} is calculated on {@code long} data type numbers
     * so the precision may vary. The last measurement time field may be different from {@code base + timeSpan},
     * which is the timestamp, the last measurement should have (with 100% calculation precision).
     *
     * @param measurements list of {@link Measurement}s to adjust their times
     * @param timeSpan     time span in UNIX milliseconds used to get all provided measurements
     */
    void approximate(@NonNull List<Measurement> measurements, long timeSpan);
}
