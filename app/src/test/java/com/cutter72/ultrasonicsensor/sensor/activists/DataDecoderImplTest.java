package com.cutter72.ultrasonicsensor.sensor.activists;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DataDecoderImplTest {

    @Test
    public void decodeDataFromSensor() {
        //GIVEN
        byte[] rawData = new byte[128];
        for (int i = 0; i < rawData.length; i++) {
            if ((i + 1) % 6 == 0) {
                rawData[i] = 13;
            } else {
                rawData[i] = (byte) i;
            }
        }
        rawData[0] = -1;
        //THEN
        List<Measurement> decodedMeasurements = new DataDecoderImpl().decodeDataFromSensor(rawData);
        //WHEN
        assertEquals(10.61, decodedMeasurements.get(0).getDistanceCentimeters(), 0.0);
        assertEquals(583.62, decodedMeasurements.get(1).getDistanceCentimeters(), 0.0);
    }
}