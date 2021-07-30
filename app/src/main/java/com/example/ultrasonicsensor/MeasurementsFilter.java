package com.example.ultrasonicsensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MeasurementsFilter {

    public List<Measurement> filterByMedian(List<Measurement> measurementsToFilter, double maxDeviationFromMedianInCentimeters) {
        List<Measurement> filteredResult = new ArrayList<>(measurementsToFilter);
        double median;
        int filteredOutMeasurements = 0;
        int measurementsBufferSize = filteredResult.size();
        Collections.sort(filteredResult);
        if (measurementsBufferSize % 2 == 0) {
            int index = measurementsBufferSize / 2 - 1;
            median = (filteredResult.get(index).getCentimetersDistance() + filteredResult.get(++index).getCentimetersDistance()) / 2;
        } else {
            int index = (measurementsBufferSize + 1) / 2 - 1;
            median = filteredResult.get(index).getCentimetersDistance();
        }
        System.out.println("Median: " + median);
        System.out.println("Max deviation: " + maxDeviationFromMedianInCentimeters);
        for (Measurement measurement : measurementsToFilter) {
            if (Math.abs(measurement.getCentimetersDistance() - median) > maxDeviationFromMedianInCentimeters) {
                filteredOutMeasurements++;
                filteredResult.remove(measurement);
//                System.out.println("Measurement removed: " + measurement);
            }
        }
        System.out.println("Filtered out measurements: " + filteredOutMeasurements);
        return filteredResult;
    }
}
