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
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("Convert2Lambda")
public class MainActivity extends AppCompatActivity {
    private static final double CENTIMETERS_UNIT_FACTOR = 0.00859536;
    public static MainActivity instance;
    public static boolean isRecording = false;
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
    private List<Impacts> impacts = new ArrayList<>();
    private double[] measurementsBuffer = new double[22];
    private int measurementsBufferCursor = 0;
    private final int timeOutMillis = 10;
    private final int maxConsoleLines = 999;
    private boolean isRawDataLogEnabled = false;

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
//        isRecording = false;
    }

    public void onClickOpenConnection(View view) {
        if (isOpened) {
            isRecording = false;
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
                btnAutoPrint.setText(R.string.start_recording);
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
        byte[] readBuffer = new byte[39];
        if (port != null) {
            try {
                port.read(readBuffer, timeOutMillis);
                if (isRawDataLogEnabled) {
                    consoleView.println(Arrays.toString(readBuffer));
                }
                int[] decimals = new int[5];
                int counter = 0;
                int sensorUnits;
                double distanceInCentimeters;
                for (byte b : readBuffer) {
                    if (b == 0) {
                        return;
                    }
                    if (b < 48 || b > 57 || counter > 4) {
                        counter = 0;
                        decimals = new int[5];
                    } else {
                        decimals[counter] = b - 48;
                        counter++;
                    }
                    if (counter - 1 == 4) {
                        sensorUnits = decimals[0] * 10000 + decimals[1] * 1000 + decimals[2] * 100 + decimals[3] * 10 + decimals[4];
                        distanceInCentimeters = Math.round(sensorUnits * CENTIMETERS_UNIT_FACTOR * 100) / 100.0;
                        Measurement measurement = new Measurement(distanceInCentimeters);
                        allMeasurements.add(measurement);
                        measurementsBuffer[measurementsBufferCursor] = (distanceInCentimeters);
                        measurementsBufferCursor++;
                        counter = 0;
                        decimals = new int[5];
                    }
                    if (measurementsBufferCursor >= measurementsBuffer.length) {
                        consoleView.println(Arrays.toString(measurementsBuffer));
                        measurementsBufferCursor = 0;
                        measurementsBuffer = new double[measurementsBuffer.length];
                        updateCounterView();
                    }
                }
            } catch (IOException | NullPointerException ex) {
                ex.printStackTrace();
//                CrashReporter.logException(ex);
                consoleView.println(ex);
            }
        } else {
            consoleView.println("port == null");
        }
    }

    private void countImpacts(Measurement measurement) {
        double difference = allMeasurements.get(allMeasurements.size() - 1).getCentimetersDistance() - measurement.getCentimetersDistance();
    }

    private void updateCounterView() {
        ((TextView) findViewById(R.id.measurementsCounter)).setText(String.valueOf(allMeasurements.size()));
    }

    public void onClickRawDataLogEnabled(View view) {
        consoleView.println("---onClickRawDataShowEnabled");
        isRawDataLogEnabled = !isRawDataLogEnabled;
        if (isRawDataLogEnabled) {
            ((Button) findViewById(R.id.btnRawData)).setText(R.string.hide_raw_data);
        } else {
            ((Button) findViewById(R.id.btnRawData)).setText(R.string.show_raw_data);
        }
    }

    public void onClickReset(View view) {
        isRecording = false;
        consoleView.clear();
        consoleView.println("---onClickReset");
        closeConnection();
        allMeasurements.clear();
        updateCounterView();
        consoleView.println("DATA CLEARED");
        ((Button) findViewById(R.id.btnAutoPrint)).setText(R.string.start_recording);
        view.setBackgroundColor(btnBackgroundColor);
    }

    @SuppressWarnings({"BusyWait"})
    public void onClickAutoPrint(View view) {
        consoleView.println("---onClickAutoPrint");
        if (port != null) {
            try {
                port.purgeHwBuffers(true, true);
            } catch (IOException | NullPointerException ex) {
                ex.printStackTrace();
                consoleView.println(ex);
            }
        }
        if (isOpened) {
            if (isRecording) {
                consoleView.println("STOP RECORD");
                isRecording = false;
                ((Button) view).setText(R.string.start_recording);
                view.setBackgroundColor(btnBackgroundColor);
            } else {
                consoleView.println("START RECORD");
                isRecording = true;
                Runnable delayedRunnable = new Runnable() {
                    @Override
                    public void run() {
                        while (isRecording) {
                            try {
                                Thread.sleep(timeOutMillis);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MainActivity.instance.onClickPrintData(null);
                                    if (consoleView.getSize() > maxConsoleLines) {
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
                ((Button) view).setText(R.string.stop_recording);
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