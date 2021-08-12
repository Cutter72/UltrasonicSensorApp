package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrierImpl;

import java.util.ArrayList;
import java.util.List;

public class DataFilterImpl implements DataFilter {

    @NonNull
    @Override
    public SensorDataCarrier filterByMedian(@NonNull SensorDataCarrier dataToFilter, double maxDeviationFromMedianInCentimeters) {
        List<Measurement> filteredMeasurements = new ArrayList<>(dataToFilter.getRawMeasurements());
        filterOutZeroMeasurements(filteredMeasurements);
        filterOutByDeviationFromMedian(filteredMeasurements, maxDeviationFromMedianInCentimeters);
        return new SensorDataCarrierImpl()
                .setRawData(dataToFilter.getRawData())
                .setRawMeasurements(filteredMeasurements);
    }

    private void filterOutZeroMeasurements(@NonNull List<Measurement> measurementsToFilter) {
        if (measurementsToFilter.size() > 0) {
            List<Measurement> zeroMeasurements = new ArrayList<>();
            for (Measurement measurement : measurementsToFilter) {
                if (measurement.getDistanceCentimeters() == 0.0) {
                    zeroMeasurements.add(measurement);
                }
            }
            measurementsToFilter.removeAll(zeroMeasurements);
        }
    }

    private void filterOutByDeviationFromMedian(@NonNull List<Measurement> measurementsToFilter, double maxDeviationFromMedianInCentimeters) {
        if (measurementsToFilter.size() > 0) {
            double median = findMedian(measurementsToFilter);
            List<Measurement> measurementsToRemove = new ArrayList<>();
            for (Measurement measurement : measurementsToFilter) {
                if (Math.abs(measurement.getDistanceCentimeters() - median) > maxDeviationFromMedianInCentimeters) {
                    measurementsToRemove.add(measurement);
                }
            }
            measurementsToFilter.removeAll(measurementsToRemove);
        }
    }

    private double findMedian(@NonNull List<Measurement> measurementsToFilter) {
        List<Measurement> sortedMeasurements = new ArrayList<>(measurementsToFilter);
        new SorterImpl().sortByDistance(sortedMeasurements);
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
}
