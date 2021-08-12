package com.cutter72.ultrasonicsensor.sensor.activists;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SorterImplTest {
    private List<Measurement> sortedMeasurements;
    private Measurement measurement0;
    private Measurement measurement1;
    private Measurement measurement2;
    private Measurement measurement3;
    private Measurement measurement4;

    @Before
    public void setUp() {
        //GIVEN
        sortedMeasurements = new ArrayList<>();
        Date zeroDate = new Date(0);
        measurement0 = new Measurement(0.0).setDate(zeroDate);
        measurement1 = new Measurement(0.1).setDate(zeroDate);
        measurement2 = new Measurement(0.2).setDate(zeroDate);
        measurement3 = new Measurement(0.3).setDate(zeroDate);
        measurement4 = new Measurement(0.4).setDate(zeroDate);
        sortedMeasurements.add(measurement0);
        sortedMeasurements.add(measurement1);
        sortedMeasurements.add(measurement2);
        sortedMeasurements.add(measurement3);
        sortedMeasurements.add(measurement4);
    }

    @Test
    public void sortByDistance() {
        //GIVEN
        // setUp()
        List<Measurement> unsortedMeasurements = new ArrayList<>();
        unsortedMeasurements.add(measurement4);
        unsortedMeasurements.add(measurement0);
        unsortedMeasurements.add(measurement2);
        unsortedMeasurements.add(measurement1);
        unsortedMeasurements.add(measurement3);
        //WHEN
        new SorterImpl().sortByDistance(unsortedMeasurements);
        //THEN
        assertEquals(sortedMeasurements, unsortedMeasurements);
    }

}