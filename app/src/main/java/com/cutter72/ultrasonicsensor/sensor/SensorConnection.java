package com.cutter72.ultrasonicsensor.sensor;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;

/**
 * Interface for connect and read data from Senix ToughSonic sensor via USB UART RS-232 port.
 *
 * <b>IMPORTANT!</b> Sensor must be set to ASCII streaming mode.
 * <p>
 * Sensor manufacturer website: <a href="https://senix.com/">https://senix.com/</a>
 */
public interface SensorConnection {
    boolean open();

    boolean isOpen();

    @NonNull
    SensorDataCarrier readData();

    @NonNull
    SensorDataCarrier readData(@NonNull byte[] buffer);

    void close();

    boolean clearHardwareInputOutputBuffers();
}
