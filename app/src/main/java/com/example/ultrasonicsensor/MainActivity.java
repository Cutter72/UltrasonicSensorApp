package com.example.ultrasonicsensor;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.balsikandar.crashreporter.CrashReporter;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName() + ": ";

    private List<UsbSerialDriver> availableDrivers;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;

    private ConsoleView consoleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        consoleView = new ConsoleView(findViewById(R.id.linearLayout), findViewById(R.id.scrollView));
        consoleView.println("Console view created.");
    }

    public void onClickDoSomething(View view) {
        consoleView.println(TAG + "onClickDoSomething");
        try {
            findAllAvailableDriversFromAttachedDevices();
            if (availableDrivers.size() > 0) {
                openConnectionToTheFirstAvailableDriver();
                UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
                try {
                    port.open(connection);
                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    consoleView.println(TAG + "port.open");
                    Toast.makeText(this, "port.open", Toast.LENGTH_SHORT).show();
                    port.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    consoleView.println(TAG + "IOException");
                    Toast.makeText(this, "IOException", Toast.LENGTH_SHORT).show();
                }
            } else {
                consoleView.println(TAG + "noDriversFound");
                Toast.makeText(this, "noDriversFound", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            CrashReporter.logException(ex);
        }
    }

    private void openConnectionToTheFirstAvailableDriver() {
        // Open a connection to the first available driver.
        driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            consoleView.println(TAG + "connection == null");
            Toast.makeText(this, "connection == null", Toast.LENGTH_SHORT).show();
            return;
        }
        consoleView.println(TAG + "openConnectionToTheFirstAvailableDriver");
        Toast.makeText(this, "openConnectionToTheFirstAvailableDriver", Toast.LENGTH_SHORT).show();
    }

    private void findAllAvailableDriversFromAttachedDevices() {
        // Find all available drivers from attached devices.
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            consoleView.println(TAG + "availableDrivers.isEmpty");
            Toast.makeText(this, "availableDrivers.isEmpty", Toast.LENGTH_SHORT).show();
            return;
        } else {
            for (UsbSerialDriver availableDriver : availableDrivers) {
                consoleView.println("-----");
                consoleView.println("availableDriver device: " + availableDriver.getDevice());
                consoleView.println("availableDriver ports: " + availableDriver.getPorts());
                consoleView.println("-----");
            }
        }
        consoleView.println(TAG + "findAllAvailableDriversFromAttachedDevices");
        Toast.makeText(this, "findAllAvailableDriversFromAttachedDevices", Toast.LENGTH_SHORT).show();
    }

    public void onClickPrintLine(View view) {
        consoleView.println("New line printed.");
    }
}