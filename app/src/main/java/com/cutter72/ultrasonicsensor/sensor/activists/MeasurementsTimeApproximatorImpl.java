package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.Date;
import java.util.List;

public class MeasurementsTimeApproximatorImpl implements MeasurementsTimeApproximator {

    @Override
    public void approximate(@NonNull List<Measurement> measurements, long timeSpan) {
        int measurementsSize = measurements.size();
        if (measurementsSize > 1) {
            long timeDelta = timeSpan / (measurements.size() - 1);
            Measurement firstMeasurement = measurements.get(0);
            for (int i = 0; i < measurementsSize; i++) {
                Measurement measurement = measurements.get(i);
                long newTimeStamp = firstMeasurement.getTime().getTime() + i * timeDelta;
                measurement.setTime(new Date(newTimeStamp));
            }
        }
    }
}
