package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrierImpl;

import java.util.ArrayList;
import java.util.List;

public class DataFilterImpl implements DataFilter {
    public static final double DEFAULT_FILTER_DEVIATION = 0.5;
    public static final double[] FILTER_VALUES = new double[]{0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};

    @NonNull
    @Override
    public SensorDataCarrier filterByMedian(@NonNull SensorDataCarrier dataToFilter, double maxDeviationFromMedianInCentimeters) {
        List<Measurement> filteredMeasurements = new ArrayList<>(dataToFilter.getRawMeasurements());
        double median;
        median = findMedian(filteredMeasurements);
        for (Measurement measurement : dataToFilter.getRawMeasurements()) {
            if (Math.abs(measurement.getDistanceCentimeters() - median) > maxDeviationFromMedianInCentimeters) {
                filteredMeasurements.remove(measurement);
            }
        }
        return new SensorDataCarrierImpl()
                .setRawData(dataToFilter.getRawData())
                .setRawMeasurements(filteredMeasurements);
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
