package com.example.ultrasonicsensor;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.balsikandar.crashreporter.CrashReporter;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Class for connect and read data from Senix ToughSonic sensor via USB RS-232 serial port
 */
public class SensorManager {
    private final double CENTIMETERS_UNIT_FACTOR = 0.00859536; //value in centimeters from ToughSonic Sensor 12 data sheet
    //RS-232 connection params
    private final int BAUD_RATE = 9600;
    private final int DATA_BITS = 8;
    private final int BUFFER_TIME_OUT = 100;
    private final int BUFFER_SIZE = 99;

    private final Context context;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private UsbSerialPort port;
    private List<UsbSerialDriver> availableDrivers;
    private List<Measurement> allMeasurements;

    public SensorManager(Context context) {
        this.context = context;
    }

    public boolean isSensorConnected() {
        if (port != null) {
            return port.isOpen();
        }
        return false;
    }

    public void openConnectionToSensor() {
        try {
            if (availableDrivers.size() > 0) {
                openConnectionToTheFirstAvailableDriver();
            } else {
                findAllAvailableUsbDriversFromAttachedDevices();
            }
        } catch (Exception e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        }
    }

    private void findAllAvailableUsbDriversFromAttachedDevices() {
        // Find all available drivers from attached devices.
        System.out.println("findAllAvailableDriversFromAttachedDevices");
        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            System.out.println("noDriversFound");
        } else {
            for (int i = 0; i < availableDrivers.size(); i++) {
                UsbSerialDriver availableDriver = availableDrivers.get(i);
                System.out.println("---device---" + i);
                System.out.println("availableDriver device: " + availableDriver.getDevice());
                System.out.println("availableDriver ports: " + availableDriver.getPorts());
                System.out.println("------");
            }
        }
    }

    private void openConnectionToTheFirstAvailableDriver() {
        // Open a connection to the first available driver.
        System.out.println("openConnectionToTheFirstAvailableDriver");
        driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());
        if (connection != null) {
            System.out.println("CONNECTION OPEN");
            openPort();
        } else {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            System.out.println("connection == null");
        }
    }

    private void openPort() {
        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(BAUD_RATE, DATA_BITS, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            System.out.println("PORT OPEN");
        } catch (IOException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                port.close();
            } catch (IOException e) {
                e.printStackTrace();
                CrashReporter.logException(e);
            }
            connection.close();
        }
        System.out.println("CONNECTION CLOSED");
    }

    private byte[] readSensorData() {
        byte[] readDataBuffer = new byte[BUFFER_SIZE];
        if (port != null) {
            if (port.isOpen()) {
                try {
                    port.read(readDataBuffer, BUFFER_TIME_OUT);
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                    CrashReporter.logException(e);
                }
            } else {
                System.out.println("port.isOpen == false");
            }
        } else {
            openConnectionToSensor();
        }
        return readDataBuffer;
    }

    private void processInputDataFromSensor() {
        byte[] readDataBuffer = readSensorData();
        List<Integer> rawSensorUnitsBuffer = Collections.synchronizedList(new LinkedList<>());
        for (byte b : readDataBuffer) {
            if (b != 0) {
                if (b == 13) {
                    if (rawSensorUnitsBuffer.size() == 5) {
                        Measurement measurement = mergeSensorRawDataIntoCentimeterMeasurement(rawSensorUnitsBuffer);
                        allMeasurements.add(measurement);
                    }
                    rawSensorUnitsBuffer = Collections.synchronizedList(new LinkedList<>());
                } else {
                    int decodedDecimalNumber = decodeDecimalNumber(b);
                    rawSensorUnitsBuffer.add(decodedDecimalNumber);
                }
            } else {
                break;
            }
        }
    }

    private Measurement mergeSensorRawDataIntoCentimeterMeasurement(List<Integer> rawSensorUnitsBuffer) {
        int sensorUnits = mergeDataIntoSensorUnits(rawSensorUnitsBuffer);
        double distance = calculateDistance(sensorUnits);
        return new Measurement(distance);
    }

    private int decodeDecimalNumber(byte b) {
        return b - 48;
    }

    private double calculateDistance(int sensorUnits) {
        return Math.round(sensorUnits * CENTIMETERS_UNIT_FACTOR * 100) / 100.0;
    }

    private int mergeDataIntoSensorUnits(List<Integer> rawSensorUnitsBuffer) {
        return rawSensorUnitsBuffer.get(0) * 10000 +
                rawSensorUnitsBuffer.get(1) * 1000 +
                rawSensorUnitsBuffer.get(2) * 100 +
                rawSensorUnitsBuffer.get(3) * 10 +
                rawSensorUnitsBuffer.get(4);
    }
}
