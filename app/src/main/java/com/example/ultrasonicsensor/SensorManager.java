package com.example.ultrasonicsensor;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.balsikandar.crashreporter.CrashReporter;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for connect and read data from Senix ToughSonic sensor via USB RS-232 port. Sensor must be
 * set to ASCII streaming mode.
 */
public class SensorManager {
    private final double CENTIMETERS_UNIT_FACTOR = 0.00859536; //value in centimeters from ToughSonic Sensor 12 data sheet
    // RS-232 connection params
    private final int BAUD_RATE = 9600;
    private final int DATA_BITS = 8;
    private final int BUFFER_TIME_OUT = 100;
    private final int BUFFER_SIZE = 99;
    private final int CR = 13; // Carriage return, end of data sequence
    // usb device
    private final String MANUFACTURER_NAME = "FTDI";
    private final String PRODUCT_NAME = "FT232R USB UART";

    private final Context context;
    private UsbManager manager;
    private UsbSerialDriver sensorUsbDeviceDriver;
    private UsbDevice sensorUsbDevice;
    private UsbDeviceConnection sensorUsbDeviceConnection;
    private UsbSerialPort sensorUsbDevicePort;
    private int MANTISSA_POWER_4 = 10000;
    private int MANTISSA_POWER_3 = 1000;
    private int MANTISSA_POWER_2 = 100;
    private int MANTISSA_POWER_1 = 10;
    private int MANTISSA_POWER_0 = 1;

    public SensorManager(Context context) {
        this.context = context;
    }

    public boolean isSensorConnectionOpen() {
        System.out.println("isSensorConnectionOpen");
        if (sensorUsbDevicePort != null) {
            return sensorUsbDevicePort.isOpen();
        }
        return false;
    }

    public boolean openConnectionToSensor() {
        System.out.println("openConnectionToSensor");
        if (openSensorPort()) {
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

    public void closeConnectionToSensor() {
        closeSensorPort();
        closeSensorUsbDeviceConnection();
        System.out.println("CONNECTION CLOSED");
    }

    private void closeSensorUsbDeviceConnection() {
        if (sensorUsbDeviceConnection != null) {
            sensorUsbDeviceConnection.close();
        }
    }

    private void closeSensorPort() {
        if (sensorUsbDevicePort != null) {
            try {
                sensorUsbDevicePort.close();
            } catch (IOException e) {
                e.printStackTrace();
                CrashReporter.logException(e);
            }
        }
    }

    private boolean openSensorPort() {
        System.out.println("openSensorPort");
        sensorUsbDevicePort = sensorUsbDeviceDriver.getPorts().get(0);
        if (sensorUsbDevicePort != null) {
            if (sensorUsbDeviceConnection != null) {
                try {
                    sensorUsbDevicePort.open(sensorUsbDeviceConnection);
                    sensorUsbDevicePort.setParameters(BAUD_RATE, DATA_BITS, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
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

    private boolean openSensorConnection() {
        System.out.println("openSensorUsbDeviceConnection");
        if (manager != null) {
            if (sensorUsbDevice != null) {
                sensorUsbDeviceConnection = manager.openDevice(sensorUsbDevice);
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

    private boolean findSensor() {
        System.out.println("findSensor");
        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (manager != null) {
            for (UsbSerialDriver availableDriver : UsbSerialProber.getDefaultProber().findAllDrivers(manager)) {
                UsbDevice usbDevice = availableDriver.getDevice();
                if (MANUFACTURER_NAME.equals(usbDevice.getManufacturerName())
                        && PRODUCT_NAME.equals(usbDevice.getProductName())) {
                    System.out.println("SensorUsbDeviceFound");
                    sensorUsbDeviceDriver = availableDriver;
                    sensorUsbDevice = usbDevice;
                    return true;
                }
            }
        } else {
            System.out.println("manager == null");
            return false;
        }
        System.out.println("noSensorUsbDeviceFound");
        return false;
    }

    private byte[] readRawDataFromSensor() {
        System.out.println("readSensorData");
        byte[] rawDataFromSensor = new byte[BUFFER_SIZE];
        try {
            sensorUsbDevicePort.read(rawDataFromSensor, BUFFER_TIME_OUT);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
            return null;
        }
        return rawDataFromSensor;
    }

    public List<Measurement> readMeasurementsFromSensor() {
        System.out.println("readMeasurementsFromSensor");
        List<Measurement> measurements = new ArrayList<>();
        byte[] rawDataFromSensor = readRawDataFromSensor();
        if (isSensorConnectionOpen()) {
            if (rawDataFromSensor != null) {
                List<Integer> rawSensorUnitsBuffer = new ArrayList<>();
                for (byte b : rawDataFromSensor) {
                    if (b != 0) {
                        if (b == CR) {
                            if (rawSensorUnitsBuffer.size() == 5) {
                                Measurement measurement = decodeMeasurement(rawSensorUnitsBuffer);
                                measurements.add(measurement);
                            }
                            rawSensorUnitsBuffer.clear();
                        } else {
                            int decodedDecimalNumber = decodeDecimalNumber(b);
                            rawSensorUnitsBuffer.add(decodedDecimalNumber);
                        }
                    } else {
                        break;
                    }
                }
            } else {
                System.out.println("rawDataFromSensor == null");
            }
        } else {
            reconnectToSensor();
        }
        return measurements;
    }

    private void reconnectToSensor() {
        if (openConnectionToSensor()) {
            System.out.println("reconnectionToSensorSuccess");
        } else {
            System.out.println("reconnectionToSensorFailed");
        }
    }

    private Measurement decodeMeasurement(List<Integer> rawSensorUnitsBuffer) {
        System.out.println("mergeSensorRawDataIntoCentimeterMeasurement");
        int sensorUnits = decodeSensorUnits(rawSensorUnitsBuffer);
        double distance = decodeDistance(sensorUnits);
        return new Measurement(distance);
    }

    private int decodeDecimalNumber(byte b) {
        System.out.println("decodeDecimalNumber");
        return b - 48;
    }

    private double decodeDistance(int sensorUnits) {
        System.out.println("decodeDistance");
        return Math.round(sensorUnits * CENTIMETERS_UNIT_FACTOR * 100) / 100.0;
    }

    private int decodeSensorUnits(List<Integer> rawSensorUnitsBuffer) {
        System.out.println("decodeSensorUnits");
        return rawSensorUnitsBuffer.get(0) * MANTISSA_POWER_4 + // tens thousands
                rawSensorUnitsBuffer.get(1) * MANTISSA_POWER_3 + // thousands
                rawSensorUnitsBuffer.get(2) * MANTISSA_POWER_2 + // hundreds
                rawSensorUnitsBuffer.get(3) * MANTISSA_POWER_1 + // tens
                rawSensorUnitsBuffer.get(4) * MANTISSA_POWER_0; // integers
    }
}
