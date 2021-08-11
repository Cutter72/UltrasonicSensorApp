package com.cutter72.ultrasonicsensor.sensor.solids;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MeasurementTest {

    @Test
    public void equals() {
        //GIVEN
        Date zeroDate = new Date();
        Measurement expected = new Measurement(0.0).setDate(zeroDate);
        Measurement sameDateAndDistance = new Measurement(0.0).setDate(zeroDate);
        Measurement differentDistance = new Measurement(1.0);
        Measurement.resetId();
        Measurement sameIdDateAndDistance = new Measurement(0.0).setDate(zeroDate);
        //THEN
        //WHEN
        assertNotEquals(expected, sameDateAndDistance);
        assertNotEquals(expected, null);
        assertNotEquals(expected, differentDistance);
        assertEquals(expected, sameIdDateAndDistance);
    }
}