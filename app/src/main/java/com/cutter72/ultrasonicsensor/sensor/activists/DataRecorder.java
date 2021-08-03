package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public interface DataRecorder {
    void startRecording(@NonNull UsbSerialPort portToListen);

    boolean isRecording();

    void stopRecording();
}
