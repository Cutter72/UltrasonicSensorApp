package com.cutter72.ultrasonicsensor.sensor;

import androidx.annotation.NonNull;

import java.io.Closeable;

/**
 * Interface for connect and read data from Senix ToughSonic sensor via USB UART RS-232 port.
 *
 * <b>IMPORTANT!</b> Sensor must be set to ASCII streaming mode.
 * <p>
 * Sensor manufacturer website: <a href="https://senix.com/">https://senix.com/</a>
 */
public interface SensorConnection extends Closeable {
    boolean open();

    boolean isOpen();

    @NonNull
    byte[] readRawData();

    @NonNull
    byte[] readRawData(@NonNull byte[] buffer);

    boolean clearHardwareInputOutputBuffers();
}
