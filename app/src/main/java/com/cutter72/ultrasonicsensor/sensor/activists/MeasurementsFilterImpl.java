package com.cutter72.ultrasonicsensor.sensor.activists;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MeasurementsFilterImpl implements MeasurementsFilter {

    @Override
    public List<Measurement> filterByMedian(List<Measurement> measurementsToFilter, double maxDeviationFromMedianInCentimeters) {
        List<Measurement> filteredResult = new ArrayList<>(measurementsToFilter);
        double median;
        median = findMedian(filteredResult);
        for (Measurement measurement : measurementsToFilter) {
            if (Math.abs(measurement.getCentimetersDistance() - median) > maxDeviationFromMedianInCentimeters) {
                filteredResult.remove(measurement);
            }
        }
        return filteredResult;
    }

    private double findMedian(List<Measurement> filteredResult) {
        double median;
        int measurementsBufferSize = filteredResult.size();
        Collections.sort(filteredResult);
        if (measurementsBufferSize % 2 == 0) {
            int index = measurementsBufferSize / 2 - 1;
            median = (filteredResult.get(index).getCentimetersDistance() + filteredResult.get(++index).getCentimetersDistance()) / 2;
        } else {
            int index = (measurementsBufferSize + 1) / 2 - 1;
            median = filteredResult.get(index).getCentimetersDistance();
        }
        return median;
    }
}
