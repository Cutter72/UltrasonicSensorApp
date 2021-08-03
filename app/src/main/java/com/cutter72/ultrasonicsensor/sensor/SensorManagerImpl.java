package com.cutter72.ultrasonicsensor.sensor;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.balsikandar.crashreporter.CrashReporter;
import com.cutter72.ultrasonicsensor.sensor.activists.SensorDataDecoder;
import com.cutter72.ultrasonicsensor.sensor.activists.SensorDataDecoderImpl;
import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for connect and read data from Senix ToughSonic sensor via USB UART RS-232 port. Sensor
 * must be set to ASCII streaming mode.
 */
public class SensorManagerImpl implements SensorManager {
    // RS-232 connection params
    private final int BAUD_RATE = 9600;
    private final int DATA_BITS = 8;
    private final int BUFFER_TIME_OUT = 100;
    private final int BUFFER_SIZE = 99;
    // usb device params
    private final String MANUFACTURER_NAME = "FTDI";
    private final String PRODUCT_NAME = "FT232R USB UART";

    // connection objects
    private UsbManager usbManager;
    private UsbSerialDriver sensorUsbDeviceDriver;
    private UsbDevice sensorUsbDevice;
    private UsbDeviceConnection sensorUsbDeviceConnection;
    private UsbSerialPort sensorUsbSerialPort;

    public SensorManagerImpl(UsbManager usbManager) {
        this.usbManager = usbManager;
    }

    public UsbSerialPort getSensorUsbSerialPort() {
        return sensorUsbSerialPort;
    }

    @Override
    public boolean openConnectionToSensor() {
        System.out.println("openConnectionToSensor");
        if (isSensorConnectionOpen()) {
            System.out.println("connectionToSensorAlreadyOpen");
            return true;
        } else {
            if (findSensor()) {
                if (openSensorConnection()) {
                    if (openSensorPort()) {
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
        return false;
    }

    @Override
    public boolean isSensorConnectionOpen() {
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
                if (MANUFACTURER_NAME.equals(usbDevice.getManufacturerName())
                        && PRODUCT_NAME.equals(usbDevice.getProductName())) {
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
                    sensorUsbSerialPort.setParameters(BAUD_RATE, DATA_BITS, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
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

    @Override
    public List<Measurement> readMeasurementsFromSensor() {
        System.out.println("readMeasurementsFromSensor");
        List<Measurement> measurements = new ArrayList<>();
        byte[] rawDataFromSensor = readRawDataFromSensor();
        if (isSensorConnectionOpen()) {
            SensorDataDecoder sensorDataDecoder = new SensorDataDecoderImpl();
            measurements = sensorDataDecoder.decodeDataFromSensor(rawDataFromSensor);
        } else {
            if (reconnectToSensor()) {
                readMeasurementsFromSensor();
            } else {
                System.out.println("cannotReconnectToSensor");
            }
        }
        return measurements;
    }

    private byte[] readRawDataFromSensor() {
        System.out.println("readSensorData");
        byte[] rawDataFromSensor = new byte[BUFFER_SIZE];
        try {
            sensorUsbSerialPort.read(rawDataFromSensor, BUFFER_TIME_OUT);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
            return null;
        }
        return rawDataFromSensor;
    }

    private boolean reconnectToSensor() {
        if (openConnectionToSensor()) {
            System.out.println("reconnectionToSensorSuccess");
            return true;
        } else {
            System.out.println("reconnectionToSensorFailed");
            return false;
        }
    }

    public void closeConnectionToSensor() {
        closeSensorPort();
        closeSensorUsbDeviceConnection();
        System.out.println("CONNECTION CLOSED");
    }

    private void closeSensorPort() {
        if (sensorUsbSerialPort != null) {
            try {
                sensorUsbSerialPort.close();
            } catch (IOException e) {
                e.printStackTrace();
                CrashReporter.logException(e);
            }
        }
    }

    private void closeSensorUsbDeviceConnection() {
        if (sensorUsbDeviceConnection != null) {
            sensorUsbDeviceConnection.close();
        }
    }
}
