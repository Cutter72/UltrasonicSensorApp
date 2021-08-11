package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

@SuppressWarnings("JavadocReference")
public interface MeasurementsTimeApproximator {

    /**
     * Changes {@link Measurement#date} of each given measurements to approximated time
     * calculated by this method. Every chunk of measurements from sensor is received over
     * some time span needed for gather and decode raw data into measurements. This time span should
     * be linearly distributed between all measurements starting from first and ending at last
     * measurement. This method divide {@param timeSpanToDistribute} by number of measurements - 1
     * to calculate {@code TIME_DELTA} and distribute it evenly by increasing the each measurement
     * timestamp. That means, the first measurement time field {@link Measurement#date} is
     * unchanged and it's {@link Measurement#date} is a {@code BASE} timestamp for
     * subsequent measurements. Every measurement after first has {@link Measurement#date} set to
     * {@code new Date(BASE + i * TIME_DELTA)} where {@code i} is subsequent measurement number and
     * {@code TIME_DELTA} is a {@param timeSpanToDistribute} divided by number of measurements - 1.
     * The last measurement time field is set to {@code new Date(BASE + }{@param timeSpanToDistribute}{@code )}.
     *
     * @param measurements         list of {@link Measurement}s to adjust their times
     * @param timeSpanToDistribute time span to distribute over given measurements in milliseconds
     */
    void approximate(@NonNull List<Measurement> measurements, int timeSpanToDistribute);
}
