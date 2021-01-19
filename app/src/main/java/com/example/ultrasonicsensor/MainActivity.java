package com.example.ultrasonicsensor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("Convert2Lambda")
public class MainActivity extends AppCompatActivity {
    private static final double CENTIMETERS_UNIT_FACTOR = 0.00859536;
    public static MainActivity instance;
    public static boolean isPrinting = false;
    public static boolean isOpened = false;

    private List<UsbSerialDriver> availableDrivers;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private UsbSerialPort port;

    private ConsoleView consoleView;
    private Drawable btnBackgroundDrawable;
    private int btnBackgroundColor;
    private List<Measurement> allMeasurements = new ArrayList<>();

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        consoleView = new ConsoleView(findViewById(R.id.linearLayout), findViewById(R.id.scrollView));
        consoleView.println("Console view created.");
        instance = this;
        Button btnAutoPrint = findViewById(R.id.btnAutoPrint);
        btnBackgroundDrawable = btnAutoPrint.getBackground();
        btnBackgroundColor = btnAutoPrint.getBackgroundTintList().getColorForState(btnBackgroundDrawable.getState(), R.color.purple_500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPrinting = false;
    }

    public void onClickOpenConnection(View view) {
        if (isOpened) {
            isPrinting = false;
            consoleView.println("---onClickCloseConnection");
            closeConnection();

        } else {
            consoleView.println("---onClickOpenConnection");
            mik3yConnection();
        }
    }

    public void onClickPrintData(View view) {
        if (view != null) {
            consoleView.println("---onClickPrintData");
        }
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
                    consoleView.println("PORT OPEN");
                    isOpened = true;
                    Button btnOpenConnection = findViewById(R.id.openConnection);
                    btnOpenConnection.setText(R.string.close_connection);
                    btnOpenConnection.setBackgroundColor(getColor(R.color.design_default_color_error));
                } catch (IOException ex) {
                    ex.printStackTrace();
//                    CrashReporter.logException(ex);
                    consoleView.println(ex);
                }
            } else {
                consoleView.println("noDriversFound");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
//            CrashReporter.logException(ex);
            consoleView.println(ex.toString());
        }
    }

    private void openConnectionToTheFirstAvailableDriver() {
        // Open a connection to the first available driver.
        consoleView.println("openConnectionToTheFirstAvailableDriver");
        driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            consoleView.println("connection == null");
            return;
        }
        consoleView.println("CONNECTION OPEN");
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                port.close();
                isOpened = false;
                Button btnOpenConnection = findViewById(R.id.openConnection);
                btnOpenConnection.setText(R.string.open_connection);
                btnOpenConnection.setBackgroundColor(btnBackgroundColor);
                Button btnAutoPrint = findViewById(R.id.btnAutoPrint);
                btnAutoPrint.setText(R.string.start_auto_print);
                btnAutoPrint.setBackgroundColor(btnBackgroundColor);
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.close();
        }
        consoleView.println("CONNECTION CLOSED");
    }

    private void findAllAvailableDriversFromAttachedDevices() {
        // Find all available drivers from attached devices.
        consoleView.println("findAllAvailableDriversFromAttachedDevices");
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            consoleView.println("availableDrivers.isEmpty");
        } else {
            for (int i = 0; i < availableDrivers.size(); i++) {
                UsbSerialDriver availableDriver = availableDrivers.get(i);
                consoleView.println("---device---" + i);
                consoleView.println("availableDriver device: " + availableDriver.getDevice());
                consoleView.println("availableDriver ports: " + availableDriver.getPorts());
                consoleView.println("------");
            }
        }
    }

    private void mik3yPrintData() {
        List<Double> measurements = new ArrayList<>();
        byte[] readBuffer = new byte[282];
        if (port != null) {
            try {
                port.read(readBuffer, 250);
//                consoleView.println(Arrays.toString(readBuffer));
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
                        distanceInCentimeters = sensorUnits * CENTIMETERS_UNIT_FACTOR;
                        measurements.add(distanceInCentimeters);
                        allMeasurements.add(new Measurement(distanceInCentimeters));
                        updateCounterView();
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
//                CrashReporter.logException(ex);
                consoleView.println(ex);
            }
        } else {
            consoleView.println("port == null");
        }
    }

    private void updateCounterView() {
        ((TextView) findViewById(R.id.measurementsCounter)).setText(String.valueOf(allMeasurements.size()));
    }

    public void onClickCalcAvg(View view) {
        consoleView.println("---onClickCalcAvg");
        double sum = 0;
        for (Measurement measurement : allMeasurements) {
            sum += measurement.getCentimetersDistance();
        }
        double average = sum / allMeasurements.size();
        consoleView.println(String.format(Locale.getDefault(), "Average from all %s measurements: %f cm", allMeasurements.size(), average));
    }

    public void onClickReset(View view) {
        isPrinting = false;
        consoleView.clear();
        consoleView.println("---onClickReset");
        closeConnection();
        allMeasurements.clear();
        updateCounterView();
        consoleView.println("DATA CLEARED");
        ((Button) findViewById(R.id.btnAutoPrint)).setText(R.string.start_auto_print);
        view.setBackgroundColor(btnBackgroundColor);
    }

    @SuppressWarnings({"BusyWait"})
    public void onClickAutoPrint(View view) {
        consoleView.println("---onClickAutoPrint");
        if (isOpened) {
            if (isPrinting) {
                consoleView.println("STOP READING");
                isPrinting = false;
                ((Button) view).setText(R.string.start_auto_print);
                view.setBackgroundColor(btnBackgroundColor);
            } else {
                consoleView.println("START READING");
                isPrinting = true;
                Runnable delayedRunnable = new Runnable() {
                    @Override
                    public void run() {
                        while (isPrinting) {
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
                view.setBackgroundColor(getColor(R.color.design_default_color_error));
            }
        } else {
            consoleView.println("CONNECTION IS NOT OPEN");
        }
    }

    public void onClickClearConsole(View view) {
        consoleView.clear();
        consoleView.println("---onClickClearConsole");
        consoleView.println("CONSOLE CLEARED");
    }
}