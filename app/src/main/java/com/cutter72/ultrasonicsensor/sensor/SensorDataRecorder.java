package com.cutter72.ultrasonicsensor.sensor;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public interface SensorDataRecorder {
    void startRecording(UsbSerialPort portToListen);

    void stopRecording();
}
