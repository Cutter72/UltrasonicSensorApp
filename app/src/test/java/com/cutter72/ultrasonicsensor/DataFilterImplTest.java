package com.cutter72.ultrasonicsensor;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.activists.DataFilterImpl;
import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrierImpl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class DataFilterImplTest {
    private final double MAX_DEVIATION_FROM_MEDIAN_IN_CENTIMETERS = 0.4;
    private final int MEASUREMENTS_QUANTITY = 100;

    @Test
    public void filterByMedianNormal() {
        //GIVEN
        SensorDataCarrier dataToFilter = generateInputData(generateInputMeasurements());
        //WHEN
        SensorDataCarrier filteredData = new DataFilterImpl()
                .filterByMedian(dataToFilter, MAX_DEVIATION_FROM_MEDIAN_IN_CENTIMETERS);
        //THEN
        assertEquals(MEASUREMENTS_QUANTITY, filteredData.size());
    }

    @Test
    public void filterByMedianZeroStart() {
        //GIVEN
        List<Measurement> inputMeasurements = new ArrayList<>();
        inputMeasurements.add(new Measurement(0.0));
        inputMeasurements.add(new Measurement(50.0));
        SensorDataCarrier dataToFilter = generateInputData(inputMeasurements);
        //WHEN
        SensorDataCarrier filteredData = new DataFilterImpl()
                .filterByMedian(dataToFilter, MAX_DEVIATION_FROM_MEDIAN_IN_CENTIMETERS);
        //THEN
        assertEquals(1, filteredData.size());
    }

    @Test
    public void filterByMedianZeroEnd() {
        //GIVEN
        List<Measurement> inputMeasurements = new ArrayList<>();
        inputMeasurements.add(new Measurement(50.0));
        inputMeasurements.add(new Measurement(0.0));
        SensorDataCarrier dataToFilter = generateInputData(inputMeasurements);
        //WHEN
        SensorDataCarrier filteredData = new DataFilterImpl()
                .filterByMedian(dataToFilter, MAX_DEVIATION_FROM_MEDIAN_IN_CENTIMETERS);
        //THEN
        assertEquals(1, filteredData.size());
    }

    private SensorDataCarrier generateInputData(List<Measurement> rawMeasurements) {
        return new SensorDataCarrierImpl().setRawMeasurements(rawMeasurements);
    }

    @NonNull
    private List<Measurement> generateInputMeasurements() {
        List<Measurement> inputMeasurements = new ArrayList<>();
        int badMeasurements = 0;
        Random random = new Random();
        for (int i = 0; i < MEASUREMENTS_QUANTITY; i++) {
            int dividerOne = random.nextInt(13) + 1;
            if (i % dividerOne == 0) {
                inputMeasurements.add(new Measurement(2 * random.nextDouble() + 88));
                badMeasurements++;
            }
            int dividerTwo = random.nextInt(31) + 1;
            random.nextInt(8);
            if (i % dividerTwo == 0) {
                inputMeasurements.add(new Measurement(0));
                badMeasurements++;
            }
            inputMeasurements.add(new Measurement(MAX_DEVIATION_FROM_MEDIAN_IN_CENTIMETERS * random.nextDouble() + 91));
        }
        System.out.println("Bad measurements: " + badMeasurements);
        System.out.println(Arrays.toString(inputMeasurements.toArray()));
        return inputMeasurements;
    }

    @Test
    public void filterByMedianZeroData() {
        //GIVEN
        SensorDataCarrier zeroData = new SensorDataCarrierImpl()
                .setRawMeasurements(Collections.singletonList(new Measurement(0.0)));
        //WHEN
        SensorDataCarrier filteredZeroData = new DataFilterImpl()
                .filterByMedian(zeroData, MAX_DEVIATION_FROM_MEDIAN_IN_CENTIMETERS);
        //THEN
        assertEquals(0, filteredZeroData.size());
    }

    @Test
    public void filterByMedianEmptyData() {
        //GIVEN
        SensorDataCarrier emptyData = new SensorDataCarrierImpl();
        //WHEN
        SensorDataCarrier filteredEmptyData = new DataFilterImpl()
                .filterByMedian(emptyData, MAX_DEVIATION_FROM_MEDIAN_IN_CENTIMETERS);
        //THEN
        assertEquals(0, filteredEmptyData.size());
    }
}