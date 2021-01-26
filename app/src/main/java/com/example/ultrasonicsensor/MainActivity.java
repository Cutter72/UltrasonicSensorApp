package com.example.ultrasonicsensor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.balsikandar.crashreporter.CrashReporter;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
public class MainActivity extends AppCompatActivity {
    private static final double CENTIMETERS_UNIT_FACTOR = 0.00859536; //value from ToughSonic Sensor 12 data sheet
    public static MainActivity instance;
    public static boolean isRecording = false;
    public static boolean isOpened = false;

    private List<UsbSerialDriver> availableDrivers;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private UsbSerialPort port;

    private ConsoleView consoleView;
    private int btnBackgroundColor;
    private final List<Measurement> allMeasurements = new ArrayList<>();
    private int bufferTimeOut = 100;
    private final int bufferSize = 99;
    private final int maxConsoleLines = 999;
    private boolean isRawDataLogEnabled = false;
    private byte[] readBuffer = new byte[bufferSize];
    private List<Integer> rawSensorUnitsBuffer = Collections.synchronizedList(new LinkedList<>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeLayout();
    }

    @SuppressWarnings("ConstantConditions")
    private void initializeLayout() {
        instance = this;
        consoleView = new ConsoleView(findViewById(R.id.linearLayout), findViewById(R.id.scrollView));
        consoleView.println("Console view created.");
        Button btnAutoPrint = findViewById(R.id.btnAutoPrint);
        Drawable btnBackgroundDrawable = btnAutoPrint.getBackground();
        btnBackgroundColor = btnAutoPrint.getBackgroundTintList().getColorForState(btnBackgroundDrawable.getState(), R.color.purple_500);
        ((TextView) findViewById(R.id.bufferTimeOut)).setText(String.valueOf(bufferTimeOut));
        SeekBar bufferTimeoutSeekBar = findViewById(R.id.bufferTimeoutSeekBar);
        bufferTimeoutSeekBar.setEnabled(false);
        bufferTimeoutSeekBar.setProgress((bufferTimeOut - 10) / 10);
        bufferTimeoutSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((TextView) findViewById(R.id.bufferTimeOut)).setText(String.valueOf(seekBar.getProgress() * 10 + 10));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                bufferTimeOut = seekBar.getProgress() * 10 + 10;
                ((TextView) findViewById(R.id.bufferTimeOut)).setText(String.valueOf(bufferTimeOut));
            }
        });
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

    public void onClickSaveDataToCsv(View view) {
        if (allMeasurements.size() == 0) {
            consoleView.println("NO MEASUREMENTS DATA.");
        } else {
            requestPermissions();
        }
    }

    private void createDataFile() {
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/UltrasonicSensor");
        FileOperations.prepareDirectory(directory.getAbsolutePath());
        File outputFile = new File(directory.getAbsolutePath() + File.separator + String.format("%smmnts%sbuff.csv", allMeasurements.size(), bufferTimeOut));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allMeasurements.size(); i++) {
            Measurement measurement = allMeasurements.get(i);
            sb.append(i + 1);
            sb.append(",");
            sb.append(measurement.getTime().getTime());
            sb.append(",");
            sb.append(measurement.getCentimetersDistance());
            sb.append("\n");
        }
        FileOperations.writeToFile(outputFile, sb.toString());
        consoleView.println(String.format("MEASUREMENTS DATA EXPORTED TO: %s", outputFile.getAbsolutePath()));
    }

    private static final int PERMISSION_ID = 1029;

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                },
                PERMISSION_ID
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        System.out.println("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted.
                System.out.println("Permissions granted. WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE");
                createDataFile();
            }
        }
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

    private void bufferRead() {
        readBuffer = new byte[bufferSize];
        if (port != null) {
            try {
                port.read(readBuffer, bufferTimeOut);
                if (isRawDataLogEnabled) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            consoleView.println(Arrays.toString(readBuffer));
                        }
                    });
                }
            } catch (IOException | NullPointerException ex) {
                ex.printStackTrace();
                CrashReporter.logException(ex);
            }
        } else {
            CrashReporter.logException(new RuntimeException("port == null"));
        }
    }

    private void proceedInputDataFromSensor() {
        for (byte e : readBuffer) {
            if (e != 0) {
                if (e == 13) {
                    if (rawSensorUnitsBuffer.size() == 5) {
                        mergeSensorRawDataIntoCentimeterMeasurement();
                        if ((allMeasurements.size() % 22 == 0) ^ allMeasurements.size() == 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    printLatest22MeasurementsAndUpdateCounter();
                                }
                            });
                        }
                    }
                    rawSensorUnitsBuffer = new LinkedList<>();
                } else {
                    int decodedDecimalNumber = decodeDecimalNumber(e);
                    rawSensorUnitsBuffer.add(decodedDecimalNumber);
                }
            } else {
                break;
            }
        }
    }

    private void mergeSensorRawDataIntoCentimeterMeasurement() {
        int sensorUnits = mergeDataIntoSensorUnits();
        double distance = calculateDistance(sensorUnits);
        allMeasurements.add(new Measurement(distance));
    }

    private int decodeDecimalNumber(byte b) {
        return b - 48;
    }

    private double calculateDistance(int sensorUnits) {
        return Math.round(sensorUnits * CENTIMETERS_UNIT_FACTOR * 100) / 100.0;
    }

    private int mergeDataIntoSensorUnits() {
        return rawSensorUnitsBuffer.get(0) * 10000 +
                rawSensorUnitsBuffer.get(1) * 1000 +
                rawSensorUnitsBuffer.get(2) * 100 +
                rawSensorUnitsBuffer.get(3) * 10 +
                rawSensorUnitsBuffer.get(4);
    }

    private void updateCounterView() {
        ((TextView) findViewById(R.id.measurementsCounter)).setText(String.valueOf(allMeasurements.size()));
    }

    private void printLatest22MeasurementsAndUpdateCounter() {
        updateCounterView();
        if (isRawDataLogEnabled) {
            consoleView.println("allMeasurements.size(): " + allMeasurements.size());
        }
        consoleView.println();
        for (int i = allMeasurements.size() - 22; i < allMeasurements.size(); i++) {
            consoleView.print(allMeasurements.get(i).getCentimetersDistance() + ", ");
        }
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
        rawSensorUnitsBuffer.clear();
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
                                Thread.sleep(bufferTimeOut);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            MainActivity.instance.bufferRead();
                            MainActivity.instance.proceedInputDataFromSensor();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
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