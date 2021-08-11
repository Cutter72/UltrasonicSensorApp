package com.cutter72.ultrasonicsensor.sensor.activists;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MeasurementsTimeApproximatorImplTest {
    private List<Measurement> data;
    private Measurement measurement0;
    private Measurement measurement1;
    private Measurement measurement2;
    private Measurement measurement3;
    private Measurement measurement4;

    @Before
    public void setUp() {
        //GIVEN
        data = new ArrayList<>();
        Date zeroDate = new Date(0);
        measurement0 = new Measurement(0.0).setDate(zeroDate);
        measurement1 = new Measurement(0.1).setDate(zeroDate);
        measurement2 = new Measurement(0.2).setDate(zeroDate);
        measurement3 = new Measurement(0.3).setDate(zeroDate);
        measurement4 = new Measurement(0.4).setDate(zeroDate);
        data.add(measurement0);
        data.add(measurement1);
        data.add(measurement2);
        data.add(measurement3);
        data.add(measurement4);
    }

    @Test
    public void approximate1() {
        //GIVEN
        // setUp()
        //WHEN
        new MeasurementsTimeApproximatorImpl().approximate(data, 1);
        //THEN
        assertEquals(0, measurement0.getDate().getTime());
        assertEquals(0, measurement1.getDate().getTime());
        assertEquals(1, measurement2.getDate().getTime());
        assertEquals(1, measurement3.getDate().getTime());
        assertEquals(1, measurement4.getDate().getTime());
    }

    @Test
    public void approximate2() {
        //GIVEN
        // setUp()
        //WHEN
        new MeasurementsTimeApproximatorImpl().approximate(data, 2);
        //THEN
        assertEquals(0, measurement0.getDate().getTime());
        assertEquals(1, measurement1.getDate().getTime());
        assertEquals(1, measurement2.getDate().getTime());
        assertEquals(2, measurement3.getDate().getTime());
        assertEquals(2, measurement4.getDate().getTime());
    }

    @Test
    public void approximateOneElementList() {
        //GIVEN
        // setUp()
        //WHEN
        new MeasurementsTimeApproximatorImpl().approximate(Collections.singletonList(measurement0), 2);
        //THEN
        assertEquals(0, measurement0.getDate().getTime());
    }

    @Test
    public void approximateTwoElementList() {
        //GIVEN
        List<Measurement> twoElementList = new ArrayList<>();
        twoElementList.add(measurement0);
        twoElementList.add(measurement1);
        //WHEN
        new MeasurementsTimeApproximatorImpl().approximate(twoElementList, 2);
        //THEN
        assertEquals(0, measurement0.getDate().getTime());
        assertEquals(2, measurement1.getDate().getTime());
    }

    @Test
    public void approximateZeroTimeSpan() {
        //GIVEN
        // setUp()
        //WHEN
        new MeasurementsTimeApproximatorImpl().approximate(data, 0);
        //THEN
        assertEquals(0, measurement0.getDate().getTime());
        assertEquals(0, measurement1.getDate().getTime());
        assertEquals(0, measurement2.getDate().getTime());
        assertEquals(0, measurement3.getDate().getTime());
        assertEquals(0, measurement4.getDate().getTime());
    }
}