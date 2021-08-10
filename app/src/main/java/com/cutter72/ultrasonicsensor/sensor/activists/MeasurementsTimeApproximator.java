package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public interface MeasurementsTimeApproximator {

    /**
     * Changes {@link Measurement}{@code #time} of each given measurements to approximated time
     * calculated by this method. Every chunk of measurements from sensor is received over
     * {@link com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl#DEFAULT_BUFFER_TIME_OUT_MILLIS}
     * time span plus some time to decode raw data into measurements. This time span should be
     * linearly distributed between all measurements starting from first and ending at last measurement.
     * For a careful time approximation this method divide
     * {@link com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl#DEFAULT_BUFFER_TIME_OUT_MILLIS}
     * by number of measurements - 1 to calculate {@code timeDelta} without time spent for decode
     * raw data (Reason taking only a buffer timeout was, that sometimes first measurement timestamp
     * was smaller then last measurement timestamp of a previous measurements chunk). That means
     * first measurement time field {@link Measurement}{@code #time} is unchanged and it's
     * {@link Measurement}{@code #time} is a {@code base} timestamp for subsequent measurements.
     * Every measurement after first has {@link Measurement}{@code #time} set to
     * {@code new Date(base + i * timeDelta)} where {@code i} is subsequent measurement number and
     * {@code timeDelta} is a {@link com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl#DEFAULT_BUFFER_TIME_OUT_MILLIS}
     * divided by number of measurements - 1. The last measurement time field is set to
     * {@code base + }{@link com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl#DEFAULT_BUFFER_TIME_OUT_MILLIS}.
     *
     * @param measurements list of {@link Measurement}s to adjust their times
     */
    void approximate(@NonNull List<Measurement> measurements);
}
