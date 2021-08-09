package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for decode Sensor raw data to real measurements in centimeters unit.
 */
@SuppressWarnings("FieldCanBeLocal")
public class DataDecoderImpl implements DataDecoder {
    private final double CENTIMETERS_UNIT_FACTOR = 0.00859536; // 1 sensor unit = 0.00859536 cm from ToughSonic Sensor 12 datasheet
    private final int ASCII_NUL = 0; // no data
    private final int ASCII_CR = 13; // Carriage Return, end of data sequence
    private final int ASCII_ZERO = 48; // 0
    private final int MANTISSA_BASE_POWER_0 = 1;
    private final int MANTISSA_BASE_POWER_1 = 10;
    private final int MANTISSA_BASE_POWER_2 = 100;
    private final int MANTISSA_BASE_POWER_3 = 1000;
    private final int MANTISSA_BASE_POWER_4 = 10000;

    @NonNull
    @Override
    public List<Measurement> decodeDataFromSensor(@NonNull byte[] rawDataFromSensor) {
        List<Measurement> measurements = new ArrayList<>();
        List<Integer> rawSensorUnitsBuffer = new ArrayList<>();
        for (byte b : rawDataFromSensor) {
            if (b != ASCII_NUL) {
                if (b == ASCII_CR) {
                    if (rawSensorUnitsBuffer.size() == 5) {
                        Measurement measurement = decodeMeasurement(rawSensorUnitsBuffer);
                        measurements.add(measurement);
                    }
                    rawSensorUnitsBuffer.clear();
                } else {
                    int digit = decodeDigit(b);
                    rawSensorUnitsBuffer.add(digit);
                }
            } else {
                break;
            }
        }
        return measurements;
    }

    private Measurement decodeMeasurement(List<Integer> rawSensorUnitsBuffer) {
        System.out.println("mergeSensorRawDataIntoCentimeterMeasurement");
        int sensorUnits = decodeSensorUnits(rawSensorUnitsBuffer);
        double distance = decodeDistance(sensorUnits);
        return new Measurement(distance);
    }

    private int decodeSensorUnits(List<Integer> rawSensorUnitsBuffer) {
        System.out.println("decodeSensorUnits");
        return rawSensorUnitsBuffer.get(0) * MANTISSA_BASE_POWER_4 +  // tens thousands
                rawSensorUnitsBuffer.get(1) * MANTISSA_BASE_POWER_3 + // thousands
                rawSensorUnitsBuffer.get(2) * MANTISSA_BASE_POWER_2 + // hundreds
                rawSensorUnitsBuffer.get(3) * MANTISSA_BASE_POWER_1 + // tens
                rawSensorUnitsBuffer.get(4) * MANTISSA_BASE_POWER_0;  // integers
    }

    private double decodeDistance(int sensorUnits) {
        System.out.println("decodeDistance");
        return Math.round(sensorUnits * CENTIMETERS_UNIT_FACTOR * 100) / 100.0;
    }

    private int decodeDigit(byte b) {
        System.out.println("decodeDigit");
        return b - ASCII_ZERO;
    }
}
