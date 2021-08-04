package com.cutter72.ultrasonicsensor.sensor;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;

import com.balsikandar.crashreporter.CrashReporter;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;

@SuppressWarnings("FieldCanBeLocal")
public class SensorConnectionImpl implements SensorConnection {
    // RS-232 connection params
    public final static int DEFAULT_BUFFER_TIME_OUT_MILLIS = 100;
    private final int DEFAULT_BUFFER_SIZE = 99;
    private final int DEFAULT_BAUD_RATE = 9600;
    private final int DEFAULT_DATA_BITS = 8;
    // usb device params
    private final String DEFAULT_MANUFACTURER_NAME = "FTDI";
    private final String DEFAULT_PRODUCT_NAME = "FT232R USB UART";

    // connection objects
    private final UsbManager usbManager;
    private UsbSerialDriver sensorUsbDeviceDriver;
    private UsbDevice sensorUsbDevice;
    private UsbDeviceConnection sensorUsbDeviceConnection;
    private UsbSerialPort sensorUsbSerialPort;

    public SensorConnectionImpl(UsbManager usbManager) {
        this.usbManager = usbManager;
    }

    public UsbSerialPort getSensorUsbSerialPort() {
        return sensorUsbSerialPort;
    }

    @Override
    public boolean open() {
        System.out.println("openConnectionToSensor");
        if (isOpen()) {
            System.out.println("connectionToSensorAlreadyOpen");
            return true;
        } else {
            if (findSensor()) {
                if (openSensorConnection()) {
                    if (openSensorPort()) {
                        System.out.println("connectionOpen");
                        return true;
                    } else {
                        System.out.println("cannotOpenPort");
                    }
                } else {
                    System.out.println("cannotConnectToUsbDevice");
                }
            } else {
                System.out.println("sensorNotConnected");
            }
        }
        System.out.println("cannotOpenConnection");
        return false;
    }

    @Override
    public boolean isOpen() {
        System.out.println("isSensorConnectionOpen");
        if (sensorUsbSerialPort != null) {
            return sensorUsbSerialPort.isOpen();
        }
        return false;
    }

    private boolean findSensor() {
        System.out.println("findSensor");
        if (usbManager != null) {
            int i = 0;
            for (UsbSerialDriver availableDriver : UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)) {
                UsbDevice usbDevice = availableDriver.getDevice();
                System.out.println("---device-found---" + i);
                System.out.println("availableDriver device: " + availableDriver.getDevice());
                System.out.println("availableDriver ports: " + availableDriver.getPorts());
                System.out.println("------");
                if (DEFAULT_MANUFACTURER_NAME.equals(usbDevice.getManufacturerName())
                        && DEFAULT_PRODUCT_NAME.equals(usbDevice.getProductName())) {
                    System.out.println("SensorUsbDeviceFound");
                    sensorUsbDeviceDriver = availableDriver;
                    sensorUsbDevice = usbDevice;
                    return true;
                }
                i++;
            }
        } else {
            System.out.println("manager == null");
            return false;
        }
        System.out.println("noSensorUsbDeviceFound");
        return false;
    }

    private boolean openSensorConnection() {
        System.out.println("openSensorUsbDeviceConnection");
        if (usbManager != null) {
            if (sensorUsbDevice != null) {
                sensorUsbDeviceConnection = usbManager.openDevice(sensorUsbDevice);
                System.out.println("USB DEVICE CONNECTION OPEN");
                return true;
            } else {
                System.out.println("sensorUsbDevice == null");
                return false;
            }
        } else {
            System.out.println("manager == null");
            return false;
        }
    }

    private boolean openSensorPort() {
        System.out.println("openSensorPort");
        sensorUsbSerialPort = sensorUsbDeviceDriver.getPorts().get(0);
        if (sensorUsbSerialPort != null) {
            if (sensorUsbDeviceConnection != null) {
                try {
                    sensorUsbSerialPort.open(sensorUsbDeviceConnection);
                    sensorUsbSerialPort.setParameters(DEFAULT_BAUD_RATE, DEFAULT_DATA_BITS, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    System.out.println("USB DEVICE PORT OPEN");
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    CrashReporter.logException(e);
                    return false;
                }
            } else {
                System.out.println("sensorUsbDeviceConnection == null");
                return false;
            }
        } else {
            System.out.println("sensorUsbDevicePort == null");
            return false;
        }
    }

    @NonNull
    @Override
    public byte[] readRawData() {
        return readRawData(new byte[DEFAULT_BUFFER_SIZE]);
    }

    @NonNull
    @Override
    public byte[] readRawData(@NonNull byte[] buffer) {
        System.out.println("readRawData");
        if (isOpen()) {
            try {
                sensorUsbSerialPort.read(buffer, DEFAULT_BUFFER_TIME_OUT_MILLIS);
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
                CrashReporter.logException(e);
            }
        } else {
            if (open()) {
                readRawData(buffer);
            } else {
                System.out.println("noConnectionOpenCannotReadData");
            }
        }
        return buffer;
    }

    @Override
    public void close() {
        clearHardwareInputOutputBuffers();
        closeSerialPort();
        closeUsbDeviceConnection();
        System.out.println("CONNECTION CLOSED");
    }

    private void closeSerialPort() {
        if (sensorUsbSerialPort != null) {
            try {
                sensorUsbSerialPort.close();
            } catch (IOException e) {
                e.printStackTrace();
                CrashReporter.logException(e);
            }
        }
    }

    private void closeUsbDeviceConnection() {
        if (sensorUsbDeviceConnection != null) {
            sensorUsbDeviceConnection.close();
        }
    }

    @Override
    public boolean clearHardwareInputOutputBuffers() {
        if (sensorUsbSerialPort != null) {
            try {
                sensorUsbSerialPort.purgeHwBuffers(true, true);
                return true;
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
                CrashReporter.logException(e);
            }
        }
        return false;
    }
}
