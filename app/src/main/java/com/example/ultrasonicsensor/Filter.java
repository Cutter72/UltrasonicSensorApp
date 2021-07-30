package com.example.ultrasonicsensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Filter {

    public List<Measurement> filterByMedian(List<Measurement> measurementsToFilter, double deviationInCentimeters) {
        List<Measurement> buffer = new ArrayList<>(measurementsToFilter);
        List<Measurement> measurementsToRemove = new ArrayList<>();
        double median;
        int measurementsBufferSize = buffer.size();
        Collections.sort(buffer);
        if (measurementsBufferSize % 2 == 0) {
            int index = measurementsBufferSize / 2 - 1;
            median = (buffer.get(index).getCentimetersDistance() + buffer.get(++index).getCentimetersDistance()) / 2;
        } else {
            int index = (measurementsBufferSize + 1) / 2 - 1;
            median = buffer.get(index).getCentimetersDistance();
        }
        for (Measurement measurement : buffer) {
            if (Math.abs(measurement.getCentimetersDistance() - median) > deviationInCentimeters) {
                measurementsToRemove.add(measurement);
            }
        }
        for (Measurement measurement : measurementsToRemove) {
            buffer.remove(measurement);
        }
        return buffer;
    }

    public List<Measurement> filterAllZeros(List<Measurement> measurementsToFilter) {
        List<Measurement> buffer = new ArrayList<>(measurementsToFilter);
        buffer.removeAll(Collections.singletonList(new Measurement(0)));
        return buffer;
    }
}
