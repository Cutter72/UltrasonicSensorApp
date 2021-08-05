package com.cutter72.ultrasonicsensor.sensor;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.android.other.ConsoleViewLogger;
import com.cutter72.ultrasonicsensor.android.other.ConsoleViewLoggerImpl;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrierImpl;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;

@SuppressWarnings("FieldCanBeLocal")
public class SensorConnectionImpl implements SensorConnection {
    // RS-232 connection params
    private final static String TAG = SensorConnectionImpl.class.getSimpleName();
    public final static int DEFAULT_BUFFER_TIME_OUT_MILLIS = 100;
    public static final int NO_SIGNAL_COUNTER_RESET_VALUE = 1;
    public static final int NO_SIGNAL_COUNTER_INITIAL_VALUE = 0;
    private final int DEFAULT_BUFFER_SIZE = 99;
    private final int DEFAULT_BAUD_RATE = 9600;
    private final int DEFAULT_DATA_BITS = 8;
    // usb device params
    private final String DEFAULT_MANUFACTURER_NAME = "FTDI";
    private final String DEFAULT_PRODUCT_NAME = "FT232R USB UART";

    // connection objects
    private final UsbManager usbManager;
    private final ConsoleViewLogger log;
    private UsbSerialDriver sensorUsbDeviceDriver;
    private UsbDevice sensorUsbDevice;
    private UsbDeviceConnection sensorUsbDeviceConnection;
    private UsbSerialPort sensorUsbSerialPort;

    public static int noSignalCounter = NO_SIGNAL_COUNTER_INITIAL_VALUE;

    public SensorConnectionImpl(UsbManager usbManager) {
        this.usbManager = usbManager;
        this.log = ConsoleViewLoggerImpl.getInstance();
    }

    @Override
    public boolean open() {
        log.i(TAG, "openConnectionToSensor");
        if (isOpen()) {
            log.i(TAG, "connectionToSensorAlreadyOpen");
            return true;
        } else {
            if (findSensor()) {
                if (openSensorConnection()) {
                    if (openSensorPort()) {
                        log.i(TAG, "connectionOpen");
                        return true;
                    } else {
                        log.i(TAG, "cannotOpenPort");
                    }
                } else {
                    log.i(TAG, "cannotConnectToUsbDevice");
                }
            } else {
                log.i(TAG, "sensorNotConnected");
            }
        }
        log.i(TAG, "cannotOpenConnection");
        return false;
    }

    @Override
    public boolean isOpen() {
        if (sensorUsbSerialPort != null) {
            return sensorUsbSerialPort.isOpen();
        }
        return false;
    }

    private boolean findSensor() {
        log.i(TAG, "findSensor");
        if (usbManager != null) {
            int i = 0;
            for (UsbSerialDriver availableDriver : UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)) {
                UsbDevice usbDevice = availableDriver.getDevice();
                log.i(TAG, "---device-found---" + i);
                log.i(TAG, "availableDriver device: " + availableDriver.getDevice());
                log.i(TAG, "availableDriver ports: " + availableDriver.getPorts());
                log.i(TAG, "------");
                if (DEFAULT_MANUFACTURER_NAME.equals(usbDevice.getManufacturerName())
                        && DEFAULT_PRODUCT_NAME.equals(usbDevice.getProductName())) {
                    log.i(TAG, "SensorUsbDeviceFound");
                    sensorUsbDeviceDriver = availableDriver;
                    sensorUsbDevice = usbDevice;
                    return true;
                }
                i++;
            }
        } else {
            log.i(TAG, "manager == null");
            return false;
        }
        log.i(TAG, "noSensorUsbDeviceFound");
        return false;
    }

    private boolean openSensorConnection() {
        log.i(TAG, "openSensorUsbDeviceConnection");
        if (usbManager != null) {
            if (sensorUsbDevice != null) {
                sensorUsbDeviceConnection = usbManager.openDevice(sensorUsbDevice);
                log.i(TAG, "USB DEVICE CONNECTION OPEN");
                return true;
            } else {
                log.i(TAG, "sensorUsbDevice == null");
                return false;
            }
        } else {
            log.i(TAG, "manager == null");
            return false;
        }
    }

    private boolean openSensorPort() {
        log.i(TAG, "openSensorPort");
        sensorUsbSerialPort = sensorUsbDeviceDriver.getPorts().get(0);
        if (sensorUsbSerialPort != null) {
            if (sensorUsbDeviceConnection != null) {
                try {
                    sensorUsbSerialPort.open(sensorUsbDeviceConnection);
                    sensorUsbSerialPort.setParameters(DEFAULT_BAUD_RATE, DEFAULT_DATA_BITS, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    log.i(TAG, "USB DEVICE PORT OPEN");
                    return true;
                } catch (IOException e) {
                    log.logException(TAG, e);
                    return false;
                }
            } else {
                log.i(TAG, "sensorUsbDeviceConnection == null");
                return false;
            }
        } else {
            log.i(TAG, "sensorUsbDevicePort == null");
            return false;
        }
    }

    @NonNull
    @Override
    public SensorDataCarrier readData() {
        return readData(new byte[DEFAULT_BUFFER_SIZE]);
    }

    @NonNull
    @Override
    public SensorDataCarrier readData(@NonNull byte[] buffer) {
        SensorDataCarrier data = new SensorDataCarrierImpl();
        if (sensorUsbSerialPort.isOpen()) {
            try {
                sensorUsbSerialPort.read(buffer, DEFAULT_BUFFER_TIME_OUT_MILLIS);
                data.addRawData(buffer);
                if (data.size() < 1) {
                    if (noSignalCounter % 10 == 0) {
                        noSignalCounter = NO_SIGNAL_COUNTER_RESET_VALUE;
                    } else {
                        noSignalCounter++;
                    }
                }
            } catch (IOException | NullPointerException e) {
                log.logException(TAG, e);
            }
        } else {
            log.i(TAG, "noConnectionOpenCannotReadData");
        }
        return data;
    }

    @Override
    public void close() {
        clearHardwareInputOutputBuffers();
        closeSerialPort();
        closeUsbDeviceConnection();
        clearNoSignalCounter();
        log.i(TAG, "CONNECTION CLOSED");
    }

    @Override
    public boolean clearHardwareInputOutputBuffers() {
        if (sensorUsbSerialPort != null) {
            try {
                sensorUsbSerialPort.purgeHwBuffers(true, true);
                return true;
            } catch (IOException | NullPointerException e) {
                log.logException(TAG, e);
            }
        }
        return false;
    }

    private void clearNoSignalCounter() {
        noSignalCounter = NO_SIGNAL_COUNTER_INITIAL_VALUE;
    }

    private void closeSerialPort() {
        if (sensorUsbSerialPort != null) {
            try {
                sensorUsbSerialPort.close();
                sensorUsbSerialPort = null;
            } catch (IOException e) {
                log.logException(TAG, e);
            }
        }
    }

    private void closeUsbDeviceConnection() {
        if (sensorUsbDeviceConnection != null) {
            sensorUsbDeviceConnection.close();
            sensorUsbDeviceConnection = null;
        }
    }
}
