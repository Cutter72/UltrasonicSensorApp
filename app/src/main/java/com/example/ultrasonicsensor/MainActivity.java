package com.example.ultrasonicsensor;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.balsikandar.crashreporter.CrashReporter;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("Convert2Lambda")
public class MainActivity extends AppCompatActivity {
    private static final String mik3y = "mik3y: ";
    private static final String androidUart = "android_UART: ";
    private static final double unitFactorInCentimeters = 0.00859536;
    public static MainActivity instance;
    public static AtomicBoolean isRunning = new AtomicBoolean(false);

    private List<UsbSerialDriver> availableDrivers;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private UsbSerialPort port;

    private ConsoleView consoleView;
    private List<Double> allMeasurements = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        consoleView = new ConsoleView(findViewById(R.id.linearLayout), findViewById(R.id.scrollView));
        consoleView.println("Console view created.");
        instance = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning.set(false);
    }

    public void onClickOpenConnection(View view) {
        consoleView.println("---onClickOpenConnection");
        mik3yConnection();
//        androidUartConnection();
    }

    private void androidUartConnection() {
//        PeripheralManager manager = PeripheralManager.getInstance();
//        List<String> deviceList = manager.getUartDeviceList();
//        if (deviceList.isEmpty()) {
//            consoleView.println(androidUart + "No UART port available on this device.");
//        } else {
//            consoleView.println(androidUart + "List of available devices: " + deviceList);
//        }
    }

    public void onClickPrintData(View view) {
        mik3yPrintData();
    }

    private void mik3yConnection() {
        try {
            findAllAvailableDriversFromAttachedDevices();
            if (availableDrivers.size() > 0) {
                openConnectionToTheFirstAvailableDriver();
                port = driver.getPorts().get(0); // Most devices have just one port (port 0)
                try {
                    port.open(connection);
                    port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    consoleView.println(mik3y + "PORT OPEN");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    CrashReporter.logException(ex);
                    consoleView.println(mik3y + ex);
                }
            } else {
                consoleView.println(mik3y + "noDriversFound");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            CrashReporter.logException(ex);
            consoleView.println(mik3y + ex.toString());
        }
    }

    private void openConnectionToTheFirstAvailableDriver() {
        // Open a connection to the first available driver.
        consoleView.println(mik3y + "openConnectionToTheFirstAvailableDriver");
        driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            consoleView.println(mik3y + "connection == null");
            return;
        }
        consoleView.println(mik3y + "CONNECTION OPEN");
    }

    public void onClickCloseConnection(View view) {
        consoleView.println(mik3y + "onClickCloseConnection");
        if (connection != null) {
            try {
                port.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.close();
        }
        consoleView.println(mik3y + "CONNECTION CLOSED");
    }

    private void findAllAvailableDriversFromAttachedDevices() {
        // Find all available drivers from attached devices.
        consoleView.println(mik3y + "findAllAvailableDriversFromAttachedDevices");
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            consoleView.println(mik3y + "availableDrivers.isEmpty");
        } else {
            for (int i = 0; i < availableDrivers.size(); i++) {
                UsbSerialDriver availableDriver = availableDrivers.get(i);
                consoleView.println(mik3y + "---device---" + i);
                consoleView.println(mik3y + "availableDriver device: " + availableDriver.getDevice());
                consoleView.println(mik3y + "availableDriver ports: " + availableDriver.getPorts());
                consoleView.println(mik3y + "------");
            }
        }
    }

    private void mik3yPrintData() {
        List<Double> measurements = new ArrayList<>();
        byte[] readBuffer = new byte[282];
        if (port != null) {
            try {
                port.read(readBuffer, 250);
//                consoleView.println(mik3y + Arrays.toString(readBuffer));
                int[] decimals = new int[5];
                int counter = 0;
                int sensorUnits;
                double average;
                double distanceInCentimeters;
//                consoleView.println();
                for (byte b : readBuffer) {
//                    consoleView.println(String.format(Locale.getDefault(), "counter: %s, b: %s", counter, b));
                    if (b < 48 || b > 57 || counter > 4) {
                        counter = 0;
                        decimals = new int[5];
                    } else {
                        decimals[counter] = b - 48;
//                        consoleView.println(String.format(Locale.getDefault(), "counter: %s, b: %s, decimal: %s", counter, b, decimals[counter]));
                        counter++;
                    }
                    if (counter - 1 == 4) {
                        sensorUnits = decimals[0] * 10000 + decimals[1] * 1000 + decimals[2] * 100 + decimals[3] * 10 + decimals[4];
                        distanceInCentimeters = sensorUnits * unitFactorInCentimeters;
                        measurements.add(distanceInCentimeters);
                        allMeasurements.add(distanceInCentimeters);
//                        consoleView.print(", SensorUnits: " + sensorUnits);
//                        consoleView.print(String.format(Locale.getDefault(), ", Distance %s: %f cm", measurements.size(), distanceInCentimeters));
                        counter = 0;
                        decimals = new int[5];
//                        if (measurements.size() % 3 == 0 && measurements.size() > 0) {
//                            consoleView.println();
//                        }
                    }
                }
                double sum = 0;
                for (Double aDouble : measurements) {
                    sum += aDouble;
                }
                average = sum / measurements.size();
                consoleView.println(String.format(Locale.getDefault(), "Average from %s measurements: %f cm", measurements.size(), average));
            } catch (IOException ex) {
                ex.printStackTrace();
                CrashReporter.logException(ex);
                consoleView.println(mik3y + ex);
            }
        } else {
            consoleView.println(mik3y + "port == null");
        }
    }

    public void onClickCalcAvg(View view) {
        consoleView.println(mik3y + "onClickCalcAvg");
        double sum = 0;
        for (Double aDouble : allMeasurements) {
            sum += aDouble;
        }
        double average = sum / allMeasurements.size();
        consoleView.println(String.format(Locale.getDefault(), "Average from all %s measurements: %f cm", allMeasurements.size(), average));
    }

    public void onClickReset(View view) {
        consoleView.clear();
        consoleView.println(mik3y + "onClickReset");
        onClickCloseConnection(null);
        allMeasurements.clear();
        consoleView.println("DATA CLEARED");
        isRunning.set(false);
        ((Button) findViewById(R.id.btnAutoPrint)).setText(R.string.start_auto_print);
    }

    @SuppressWarnings("BusyWait")
    public void onClickAutoPrint(View view) {
        consoleView.println("onClickAutoPrint");
        if (isRunning.get()) {
            consoleView.println("STOP READING");
            isRunning.set(false);
            ((Button) view).setText(R.string.start_auto_print);
        } else {
            consoleView.println("START READING");
            isRunning.set(true);
            Runnable delayedRunnable = new Runnable() {
                @Override
                public void run() {
                    while (isRunning.get()) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.instance.onClickPrintData(null);
                                if (consoleView.getSize() > 99) {
                                    consoleView.clear();
                                    consoleView.println("CONSOLE CLEARED");
                                }
                            }
                        });
                    }
                    try {
                        Thread.currentThread().join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            new Thread(delayedRunnable).start();
            ((Button) view).setText(R.string.stop_auto_print);
        }
    }
}