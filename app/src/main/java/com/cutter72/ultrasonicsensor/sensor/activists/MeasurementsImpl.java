package com.cutter72.ultrasonicsensor.sensor.activists;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("Convert2Lambda")
public class MeasurementsImpl implements Measurements {

    @Override
    public List<Measurement> filterByMedian(List<Measurement> measurementsToFilter, double maxDeviationFromMedianInCentimeters) {
        List<Measurement> filteredResult = new ArrayList<>(measurementsToFilter);
        double median;
        median = findMedian(measurementsToFilter);
        for (Measurement measurement : measurementsToFilter) {
            if (Math.abs(measurement.getDistanceCentimeters() - median) > maxDeviationFromMedianInCentimeters) {
                filteredResult.remove(measurement);
            }
        }
        return filteredResult;
    }

    private double findMedian(List<Measurement> measurementsToFilter) {
        List<Measurement> sortedMeasurements = this.sortByDistance(new ArrayList<>(measurementsToFilter));
        double median;
        int measurementsBufferSize = sortedMeasurements.size();
        if (measurementsBufferSize % 2 == 0) {
            int index = measurementsBufferSize / 2 - 1;
            median = (sortedMeasurements.get(index).getDistanceCentimeters() + sortedMeasurements.get(++index).getDistanceCentimeters()) / 2;
        } else {
            int index = (measurementsBufferSize + 1) / 2 - 1;
            median = sortedMeasurements.get(index).getDistanceCentimeters();
        }
        return median;
    }

    @Override
    public List<Measurement> sortByDistance(List<Measurement> measurementsToSort) {
        Collections.sort(measurementsToSort, new Comparator<Measurement>() {
            @Override
            public int compare(Measurement o1, Measurement o2) {
                return Double.compare(o1.getDistanceCentimeters(), o2.getDistanceCentimeters());
            }
        });
        return measurementsToSort;
    }

    @Override
    public List<Measurement> sortById(List<Measurement> measurementsToSort) {
        Collections.sort(measurementsToSort, new Comparator<Measurement>() {
            @Override
            public int compare(Measurement o1, Measurement o2) {
                return Integer.compare(o1.getId(), o2.getId());
            }
        });
        return measurementsToSort;
    }
}
