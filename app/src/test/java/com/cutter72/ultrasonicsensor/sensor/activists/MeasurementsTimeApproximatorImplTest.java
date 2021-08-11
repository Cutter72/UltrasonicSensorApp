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
    private Measurement msrmnt1;
    private Measurement msrmnt2;
    private Measurement msrmnt3;
    private Measurement msrmnt4;
    private Measurement msrmnt5;

    @Before
    public void setUp() {
        //GIVEN
        data = new ArrayList<>();
        Date zeroDate = new Date(0);
        msrmnt1 = new Measurement(0.1).setDate(zeroDate);
        msrmnt2 = new Measurement(0.2).setDate(zeroDate);
        msrmnt3 = new Measurement(0.3).setDate(zeroDate);
        msrmnt4 = new Measurement(0.4).setDate(zeroDate);
        msrmnt5 = new Measurement(0.5).setDate(zeroDate);
        data.add(msrmnt1);
        data.add(msrmnt2);
        data.add(msrmnt3);
        data.add(msrmnt4);
        data.add(msrmnt5);
    }

    @Test
    public void approximate1() {
        //GIVEN
        // setUp()
        //WHEN
        new MeasurementsTimeApproximatorImpl().approximate(data, 1);
        //THEN
        assertEquals(0, msrmnt1.getDate().getTime());
        assertEquals(0, msrmnt2.getDate().getTime());
        assertEquals(1, msrmnt3.getDate().getTime());
        assertEquals(1, msrmnt4.getDate().getTime());
        assertEquals(1, msrmnt5.getDate().getTime());
    }

    @Test
    public void approximate2() {
        //GIVEN
        // setUp()
        //WHEN
        new MeasurementsTimeApproximatorImpl().approximate(data, 2);
        //THEN
        assertEquals(0, msrmnt1.getDate().getTime());
        assertEquals(1, msrmnt2.getDate().getTime());
        assertEquals(1, msrmnt3.getDate().getTime());
        assertEquals(2, msrmnt4.getDate().getTime());
        assertEquals(2, msrmnt5.getDate().getTime());
    }

    @Test
    public void approximateOneElementList() {
        //GIVEN
        // setUp()
        //WHEN
        new MeasurementsTimeApproximatorImpl().approximate(Collections.singletonList(msrmnt1), 2);
        //THEN
        assertEquals(0, msrmnt1.getDate().getTime());
    }

    @Test
    public void approximateTwoElementList() {
        //GIVEN
        List<Measurement> twoElementList = new ArrayList<>();
        twoElementList.add(msrmnt1);
        twoElementList.add(msrmnt2);
        //WHEN
        new MeasurementsTimeApproximatorImpl().approximate(twoElementList, 2);
        //THEN
        assertEquals(0, msrmnt1.getDate().getTime());
        assertEquals(2, msrmnt2.getDate().getTime());
    }

    @Test
    public void approximateZeroTimeSpan() {
        //GIVEN
        // setUp()
        //WHEN
        new MeasurementsTimeApproximatorImpl().approximate(data, 0);
        //THEN
        assertEquals(0, msrmnt1.getDate().getTime());
        assertEquals(0, msrmnt2.getDate().getTime());
        assertEquals(0, msrmnt3.getDate().getTime());
        assertEquals(0, msrmnt4.getDate().getTime());
        assertEquals(0, msrmnt5.getDate().getTime());
    }
}