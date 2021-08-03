package com.cutter72.ultrasonicsensor.sensor.activists;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public interface DataRecorder {
    void startRecording(UsbSerialPort portToListen);

    void stopRecording();
}
