package com.cutter72.ultrasonicsensor.sensor.activists;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public interface SensorDataRecorder {
    void startRecording(UsbSerialPort portToListen);

    void stopRecording();
}
