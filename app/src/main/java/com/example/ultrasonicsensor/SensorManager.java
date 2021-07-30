package com.example.ultrasonicsensor;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

/**
 * Class for connect and read data from Senix ToughSonic sensor via USB RS-232 serial port
 */
public class SensorManager {
    private final Context context;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private UsbSerialPort port;
    private List<UsbSerialDriver> availableDrivers;

    public SensorManager(Context context) {
        this.context = context;
    }

    public boolean isConnectionOpen() {
        if (port != null) {
            return port.isOpen();
        }
        return false;
    }

    private void openConnection() {
        try {
            findAllAvailableUsbDriversFromAttachedDevices();
            if (availableDrivers.size() > 0) {
                openConnectionToTheFirstAvailableDriver();
                openPort();
            } else {
                System.out.println("noDriversFound");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.toString());
        }
    }

    private void findAllAvailableUsbDriversFromAttachedDevices() {
        // Find all available drivers from attached devices.
        System.out.println("findAllAvailableDriversFromAttachedDevices");
        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            System.out.println("availableDrivers.isEmpty");
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
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            System.out.println("connection == null");
            return;
        }
        System.out.println("CONNECTION OPEN");
    }

    private void openPort() {
        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            System.out.println("PORT OPEN");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                port.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.close();
        }
        System.out.println("CONNECTION CLOSED");
    }
}
