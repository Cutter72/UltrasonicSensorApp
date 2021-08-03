package com.cutter72.ultrasonicsensor.sensor;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.io.Closeable;
import java.util.List;

/**
 * Class for connect and read data from Senix ToughSonic sensor via USB UART RS-232 port. Sensor
 * must be set to ASCII streaming mode.
 */
public interface SensorConnection extends Closeable {
    boolean open();

    boolean isOpen();

    @NonNull
    List<Measurement> readMeasurementsFromSensor();

    @NonNull
    byte[] readRawData();

    @NonNull
    byte[] readRawData(@NonNull byte[] buffer);

    boolean clearHardwareInputOutputBuffers();
}
