package com.example.ultrasonicsensor;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.balsikandar.crashreporter.CrashReporter;
import com.google.android.things.pio.PeripheralManager;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String mik3y = "mik3y: ";
    private static final String androidUart = "android_UART: ";

    private List<UsbSerialDriver> availableDrivers;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private UsbSerialPort port;

    private ConsoleView consoleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        consoleView = new ConsoleView(findViewById(R.id.linearLayout), findViewById(R.id.scrollView));
        consoleView.println("Console view created.");
    }

    public void onClickOpenConnection(View view) {
        consoleView.println("---onClickOpenConnection");
//        mik3yConnection();
        androidUartConnection();
    }

    private void androidUartConnection() {
        PeripheralManager manager = PeripheralManager.getInstance();
        List<String> deviceList = manager.getUartDeviceList();
        if (deviceList.isEmpty()) {
            consoleView.println(androidUart + "No UART port available on this device.");
        } else {
            consoleView.println(androidUart + "List of available devices: " + deviceList);
        }
    }

    public void onClickPrintData(View view) {
        consoleView.println("---onClickDataPrint");
//        mik3yPrintData();
    }

    private void mik3yConnection() {
        try {
            findAllAvailableDriversFromAttachedDevices();
            if (availableDrivers.size() > 0) {
                openConnectionToTheFirstAvailableDriver();
                port = driver.getPorts().get(0); // Most devices have just one port (port 0)
                try {
                    port.open(connection);
                    port.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    consoleView.println(mik3y + "port.open");
                } catch (IOException e) {
                    e.printStackTrace();
                    consoleView.println(mik3y + "IOException");
                }
            } else {
                consoleView.println(mik3y + "noDriversFound");
            }
        } catch (Exception ex) {
            CrashReporter.logException(ex);
            consoleView.println(mik3y + ex.toString());
        }
    }

    private void openConnectionToTheFirstAvailableDriver() {
        // Open a connection to the first available driver.
        driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            consoleView.println(mik3y + "connection == null");
            return;
        }
        consoleView.println(mik3y + "openConnectionToTheFirstAvailableDriver");
    }

    private void findAllAvailableDriversFromAttachedDevices() {
        // Find all available drivers from attached devices.
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            consoleView.println(mik3y + "availableDrivers.isEmpty");
            return;
        } else {
            for (int i = 0; i < availableDrivers.size(); i++) {
                UsbSerialDriver availableDriver = availableDrivers.get(i);
                consoleView.println(mik3y + "---device---" + i);
                consoleView.println(mik3y + "availableDriver device: " + availableDriver.getDevice());
                consoleView.println(mik3y + "availableDriver ports: " + availableDriver.getPorts());
                consoleView.println(mik3y + "------");
            }
        }
        consoleView.println(mik3y + "findAllAvailableDriversFromAttachedDevices");
    }

    private void mik3yPrintData() {
        byte[] readBuffer = new byte[64];
        if (port != null) {
            try {
                port.read(readBuffer, 50);
                consoleView.println(mik3y + Arrays.toString(readBuffer));
            } catch (IOException e) {
                e.printStackTrace();
                consoleView.println(mik3y + e);
            }
        } else {
            consoleView.println(mik3y + "port == null");
        }
    }
}