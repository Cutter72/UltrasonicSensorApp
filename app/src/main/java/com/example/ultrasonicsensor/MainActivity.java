package com.example.ultrasonicsensor;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.balsikandar.crashreporter.CrashReporter;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName() + ": ";

    private List<UsbSerialDriver> availableDrivers;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private UsbSerialPort port;
    private boolean isPrinting;

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
        try {
            findAllAvailableDriversFromAttachedDevices();
            if (availableDrivers.size() > 0) {
                openConnectionToTheFirstAvailableDriver();
                port = driver.getPorts().get(0); // Most devices have just one port (port 0)
                try {
                    port.open(connection);
                    port.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    consoleView.println("port.open");
                } catch (IOException e) {
                    e.printStackTrace();
                    consoleView.println("IOException");
                }
            } else {
                consoleView.println("noDriversFound");
            }
        } catch (Exception ex) {
            CrashReporter.logException(ex);
            consoleView.println(ex.toString());
        }
    }

    private void openConnectionToTheFirstAvailableDriver() {
        // Open a connection to the first available driver.
        driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            consoleView.println("connection == null");
            return;
        }
        consoleView.println("openConnectionToTheFirstAvailableDriver");
    }

    private void findAllAvailableDriversFromAttachedDevices() {
        // Find all available drivers from attached devices.
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            consoleView.println("availableDrivers.isEmpty");
            return;
        } else {
            for (int i = 0; i < availableDrivers.size(); i++) {
                UsbSerialDriver availableDriver = availableDrivers.get(i);
                consoleView.println("---device---" + i);
                consoleView.println("availableDriver device: " + availableDriver.getDevice());
                consoleView.println("availableDriver ports: " + availableDriver.getPorts());
                consoleView.println("------");
            }
        }
        consoleView.println("findAllAvailableDriversFromAttachedDevices");
    }

    public void onClickPrintData(View view) {
        consoleView.println("---onClickDataPrint");
        byte[] readBuffer = new byte[64];
//        if (isPrinting) {
//
//        } else {
        if (port != null) {
            try {
                port.read(readBuffer, 50);
//                try {
//                    wait(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    consoleView.println(e);
//                }
                consoleView.println(Arrays.toString(readBuffer));
            } catch (IOException e) {
                e.printStackTrace();
                consoleView.println(e);
            }
        } else {
            consoleView.println("port == null");
        }
//        }
    }
}