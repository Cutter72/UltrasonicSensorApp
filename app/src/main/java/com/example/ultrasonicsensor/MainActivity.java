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
import android.widget.NumberPicker;
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

@SuppressWarnings({"Convert2Lambda"})
public class MainActivity extends AppCompatActivity {
    private static final double CENTIMETERS_UNIT_FACTOR = 0.00859536; //value in centimeters from ToughSonic Sensor 12 data sheet
    public static MainActivity instance;
    public static boolean isRecording = false;
    public static boolean isOpened = false;

    //RS232 connection
    private List<UsbSerialDriver> availableDrivers;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private UsbSerialPort port;

    //layout
    private final int SEEKBAR_MAX_VALUE = 19;
    private ConsoleView consoleView;
    private int btnBackgroundColor;

    //read and filter data
    private static final double MAX_MEASUREMENT_DEVIATION = 1.0; // in centimeters
    private final int MEASUREMENTS_BUFFER_SIZE = 5;
    private final int BUFFER_TIME_OUT = 100;
    private final int BUFFER_SIZE = 99;
    private byte[] readDataBuffer = new byte[BUFFER_SIZE];
    private List<Integer> rawSensorUnitsBuffer = Collections.synchronizedList(new LinkedList<>());
    private final List<Measurement> measurementsBuffer = new ArrayList<>();
    private final List<Measurement> allNonZeroMeasurements = new ArrayList<>();
    private final List<Measurement> allMeasurements = new ArrayList<>();
    private final List<Measurement> filteredMeasurements = new ArrayList<>();
    private final List<Measurement> filteredOutMeasurements = new ArrayList<>();
    private final List<Measurement> zeroMeasurements = new ArrayList<>();

    // print data in the console view
    private final int MEASUREMENTS_IN_ONE_LINE = 18;
    private final int CONSOLE_LINES_LIMIT = 999;
    private boolean isRawDataLogEnabled = false;

    //count impacts
    private final double MIN_DIFFERENCE_DEFAULT = 0.4;
    private final int AVG_MEASUREMENTS_DEFAULT = 4;
    private final int MIN_INTERVAL_BETWEEN_IMPACTS_MILLIS_DEFAULT = 1000;
    private final int IMPACTS_DEFAULT = 0;
    private final double[] minDiffValues = new double[]{0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
    private final int[] avgMeasurementsValues = new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private double minDifference;
    private int avgMeasurements;
    private int minTimeIntervalBetweenImpactMillis; //50ms => 20 impacts / second
    private int impacts;
    private long previousImpactTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeFields();
        initializeLayout();
    }

    private void initializeFields() {
        isRawDataLogEnabled = false;
        rawSensorUnitsBuffer = Collections.synchronizedList(new LinkedList<>());
        previousImpactTimestamp = 0;
        minDifference = MIN_DIFFERENCE_DEFAULT;
        avgMeasurements = AVG_MEASUREMENTS_DEFAULT;
        minTimeIntervalBetweenImpactMillis = MIN_INTERVAL_BETWEEN_IMPACTS_MILLIS_DEFAULT; //50ms => 20 impacts / second
        impacts = IMPACTS_DEFAULT;
    }

    private void initializeLayout() {
        instance = this;
        initializeConsoleView();
        Button btnRecording = findViewById(R.id.btnRecording);
        Drawable btnBackgroundDrawable = btnRecording.getBackground();
        btnBackgroundColor = btnRecording.getBackgroundTintList().getColorForState(btnBackgroundDrawable.getState(), R.color.purple_500);
        updateIntervalValueTextView();
        initializeSeekBar();
        initializeNumberPickers();
    }

    private void initializeConsoleView() {
        consoleView = new ConsoleView(findViewById(R.id.linearLayout), findViewById(R.id.scrollView));
        consoleView.println("Console view created.");
    }

    private void initializeSeekBar() {
        SeekBar minTimeIntervalSeekBar = findViewById(R.id.minTimeIntervalSeekBar);
        minTimeIntervalSeekBar.setMax(SEEKBAR_MAX_VALUE);
        updateIntervalSeekBarView();
        minTimeIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((TextView) findViewById(R.id.minTimeInterval)).setText(String.valueOf(seekBar.getProgress() * 50 + 50));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                minTimeIntervalBetweenImpactMillis = seekBar.getProgress() * 50 + 50;
                updateIntervalValueTextView();
            }
        });
    }

    private void initializeNumberPickers() {
        NumberPicker minDifferencePicker = findViewById(R.id.minDifferencePicker);
        minDifferencePicker.setMinValue(0);
        minDifferencePicker.setMaxValue(minDiffValues.length - 1);
        minDifferencePicker.setDisplayedValues(transformToStringArray(minDiffValues));
        updateMinDiffPickerView();
        minDifferencePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setMinDiffValue(newVal);
            }
        });
        NumberPicker avgMeasurementsPicker = findViewById(R.id.avgMeasurementsPicker);
        avgMeasurementsPicker.setMinValue(0);
        avgMeasurementsPicker.setMaxValue(avgMeasurementsValues.length - 1);
        avgMeasurementsPicker.setDisplayedValues(transformToStringArray(avgMeasurementsValues));
        updateAvgPickerView();
        avgMeasurementsPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setAvgMeasurementsValues(newVal);
            }
        });
    }

    private void updateIntervalValueTextView() {
        ((TextView) findViewById(R.id.minTimeInterval)).setText(String.valueOf(minTimeIntervalBetweenImpactMillis));
    }

    private void updateIntervalSeekBarView() {
        ((SeekBar) findViewById(R.id.minTimeIntervalSeekBar)).setProgress((minTimeIntervalBetweenImpactMillis - 50) / 50);
    }

    private void updateMinDiffPickerView() {
        ((NumberPicker) findViewById(R.id.minDifferencePicker)).setValue(getMinDiffPickerValue());
    }

    private void updateAvgPickerView() {
        ((NumberPicker) findViewById(R.id.avgMeasurementsPicker)).setValue(getAvgPickerValue());
    }

    private int getMinDiffPickerValue() {
        for (int i = 0; i < minDiffValues.length; i++) {
            if (minDiffValues[i] == minDifference) {
                return i;
            }
        }
        return 0;
    }

    private int getAvgPickerValue() {
        for (int i = 0; i < avgMeasurementsValues.length; i++) {
            if (avgMeasurementsValues[i] == avgMeasurements) {
                return i;
            }
        }
        return 0;
    }

    private void setMinDiffValue(int newVal) {
        minDifference = minDiffValues[newVal];
    }

    private void setAvgMeasurementsValues(int newVal) {
        avgMeasurements = avgMeasurementsValues[newVal];
    }

    private String[] transformToStringArray(double[] doubleTab) {
        String[] result = new String[doubleTab.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = String.valueOf(doubleTab[i]);
        }
        return result;
    }

    private String[] transformToStringArray(int[] intTab) {
        String[] result = new String[intTab.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = String.valueOf(intTab[i]);
        }
        return result;
    }

    @Override
    protected void onPause() {
        super.onPause();
//        isRecording = false;
    }

    public void onClickOpenConnection(View view) {
        if (isOpened) {
            consoleView.println("---onClickCloseConnection");
            isRecording = false;
            closeConnection();
        } else {
            consoleView.println("---onClickOpenConnection");
            mik3yConnection();
        }
    }

    public void onClickSaveDataToCsv(View view) {
        if (allMeasurements.size() == 0) {
            consoleView.println("NO MEASUREMENTS RECORDED. DATA NOT SAVED.");
        } else {
            requestPermissionsForWrite();
        }
    }

    private void createDataFile() {
        //todo think how to save all data
        FileManager fileManager = new FileManager();
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/UltrasonicSensor");
        fileManager.prepareDirectory(directory.getAbsolutePath());
        File outputFile = new File(directory.getAbsolutePath() + File.separator + String.format("%sImpacts%sMmnts%sInterval%sMinDiff%sAvgMmnts.csv",
                impacts,
                allMeasurements.size(),
                minTimeIntervalBetweenImpactMillis,
                minDifference,
                avgMeasurements));
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
        fileManager.writeToFile(outputFile, sb.toString());
        consoleView.println(String.format("MEASUREMENTS DATA EXPORTED TO: %s", outputFile.getAbsolutePath()));
    }

    private static final int PERMISSION_ID = 1029;

    private void requestPermissionsForWrite() {
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
                openPort();
            } else {
                consoleView.println("noDriversFound");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            consoleView.println(ex.toString());
        }
    }

    private void openPort() {
        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            consoleView.println("PORT OPEN");
            isOpened = true;
            updateConnectionButtonView();
        } catch (IOException ex) {
            ex.printStackTrace();
//                    CrashReporter.logException(ex);
            consoleView.println(ex);
        }
    }

    private void updateConnectionButtonView() {
        Button btnOpenConnection = findViewById(R.id.openConnection);
        if (isOpened) {
            btnOpenConnection.setText(R.string.close_connection);
            btnOpenConnection.setBackgroundColor(getColor(R.color.design_default_color_error));
        } else {
            btnOpenConnection.setText(R.string.open_connection);
            btnOpenConnection.setBackgroundColor(btnBackgroundColor);
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
                isRecording = false;
                updateConnectionButtonView();
                updateRecordingButtonView();
            } catch (IOException e) {
                System.out.println(e.getMessage());
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
        readDataBuffer = new byte[BUFFER_SIZE];
        if (port != null) {
            try {
                port.read(readDataBuffer, BUFFER_TIME_OUT);
                if (isRawDataLogEnabled) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            consoleView.println(Arrays.toString(readDataBuffer));
                        }
                    });
                }
            } catch (IOException | NullPointerException ex) {
                ex.printStackTrace();
//                CrashReporter.logException(ex);
            }
        } else {
            CrashReporter.logException(new RuntimeException("port == null"));
        }
    }

    private void processInputDataFromSensor() {
        for (byte e : readDataBuffer) {
            if (e != 0) {
                if (e == 13) {
                    if (rawSensorUnitsBuffer.size() == 5) {
                        Measurement measurement = mergeSensorRawDataIntoCentimeterMeasurement();
                        allMeasurements.add(measurement);
                        fillMeasurementsBuffer(measurement);
                        if (isMeasurementsBufferFull()) {
                            filterMeasurements();
                        }
                        if (isImpactFound()) {
                            runOnUiThread(this::updateImpactsCounterView);
                        }
                        if ((allNonZeroMeasurements.size() % MEASUREMENTS_IN_ONE_LINE == 0) ^ allNonZeroMeasurements.size() == 0) {
                            runOnUiThread(this::printLatest18MeasurementsAndUpdateCounter);
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

    private boolean isMeasurementsBufferFull() {
        return measurementsBuffer.size() == MEASUREMENTS_BUFFER_SIZE;
    }

    private void fillMeasurementsBuffer(Measurement measurement) {
        if (measurement.getCentimetersDistance() > 0) {
            if (isMeasurementsBufferFull()) {
                measurementsBuffer.remove(0);
            }
            measurementsBuffer.add(measurement);
        } else {
            filteredOutMeasurements.add(measurement);
            zeroMeasurements.add(measurement);
        }
    }

    private boolean isImpactFound() {
        if (allNonZeroMeasurements.size() > avgMeasurements) {
            double sum = 0;
            for (int i = allNonZeroMeasurements.size() - avgMeasurements - 1; i < allNonZeroMeasurements.size() - 1; i++) {
                sum += allNonZeroMeasurements.get(i).getCentimetersDistance();
            }
            double averageFromPreviousXMeasurements = sum / avgMeasurements;
            double differenceToCheck = averageFromPreviousXMeasurements - allNonZeroMeasurements.get(allNonZeroMeasurements.size() - 1).getCentimetersDistance();
            if (differenceToCheck > minDifference) {
                long currentMillis = System.currentTimeMillis();
                long timeDifference = currentMillis - previousImpactTimestamp;
                if (timeDifference >= minTimeIntervalBetweenImpactMillis) {
                    impacts++;
                    previousImpactTimestamp = currentMillis;
                    return true;
                }
            }
        }
        return false;
    }

    private void updateImpactsCounterView() {
        ((TextView) findViewById(R.id.impactsCounter)).setText(String.valueOf(impacts));
    }

    private Measurement mergeSensorRawDataIntoCentimeterMeasurement() {
        int sensorUnits = mergeDataIntoSensorUnits();
        double distance = calculateDistance(sensorUnits);
        return new Measurement(distance);
    }

    private void filterMeasurements() {
        //todo impl
        List<Measurement> buffer = new ArrayList<>(measurementsBuffer);
        Measurement oldestMeasurement = buffer.get(0);
        Measurement min = oldestMeasurement;
        Measurement max = oldestMeasurement;
        double sum = 0;
        double avg = -1;
        double median = -1;
        for (Measurement measurement : buffer) {
            sum += measurement.getCentimetersDistance();
            if (measurement.getCentimetersDistance() < min.getCentimetersDistance()) {
                min = measurement;
            } else if (measurement.getCentimetersDistance() > max.getCentimetersDistance()) {
                max = measurement;
            }
        }
        int measurementsBufferSize = buffer.size();
        avg = sum / measurementsBufferSize;
        Collections.sort(buffer);
        if (measurementsBufferSize % 2 == 0) {
            int index = measurementsBufferSize / 2 - 1;
            median = (buffer.get(index).getCentimetersDistance() + buffer.get(++index).getCentimetersDistance()) / 2;
        } else {
            int index = (measurementsBufferSize + 1) / 2 - 1;
            median = buffer.get(index).getCentimetersDistance();
        }
        for (Measurement measurement : buffer) {
            if (Math.abs(measurement.getCentimetersDistance() - median) > MAX_MEASUREMENT_DEVIATION) {
                filteredOutMeasurements.add(measurement);
                measurementsBuffer.remove(measurement);
            } else {
                filteredMeasurements.add(measurement);
            }
        }
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

    private void updateMeasurementCounterView() {
        ((TextView) findViewById(R.id.measurementsCounter)).setText(String.valueOf(allNonZeroMeasurements.size()));
    }

    private void printLatest18MeasurementsAndUpdateCounter() {
        updateMeasurementCounterView();
        if (isRawDataLogEnabled) {
            consoleView.println("allMeasurements.size(): " + allNonZeroMeasurements.size());
        }
        consoleView.println();
        for (int i = allNonZeroMeasurements.size() - MEASUREMENTS_IN_ONE_LINE; i < allNonZeroMeasurements.size(); i++) {
            consoleView.print(allNonZeroMeasurements.get(i).getCentimetersDistance() + ", ");
        }
    }

    public void onClickRawDataLog(View view) {
        consoleView.println("---onClickRawDataShow");
        isRawDataLogEnabled = !isRawDataLogEnabled;
        updateRawDataLogView();
    }

    public void onClickReset(View view) {
        consoleView.clear();
        consoleView.println("---onClickReset");
        closeConnection();
        allNonZeroMeasurements.clear();
        rawSensorUnitsBuffer.clear();
        isRawDataLogEnabled = false;
        //count impacts
        minDifference = 0.4;
        avgMeasurements = 4;
        minTimeIntervalBetweenImpactMillis = 1000;
        impacts = 0;
        previousImpactTimestamp = 0;
        updateIntervalSeekBarView();
        updateAvgPickerView();
        updateMinDiffPickerView();
        updateImpactsCounterView();
        updateMeasurementCounterView();
        updateRecordingButtonView();
        updateRawDataLogView();
        consoleView.println("DATA CLEARED");
    }

    private void updateRawDataLogView() {
        Button btnRawDataLog = findViewById(R.id.btnRawDataLog);
        if (isRawDataLogEnabled) {
            consoleView.println("RAW DATA SHOW ENABLED");
            btnRawDataLog.setText(R.string.hide_raw_data);
            btnRawDataLog.setBackgroundColor(getColor(R.color.design_default_color_error));
        } else {
            consoleView.println("RAW DATA SHOW DISABLED");
            btnRawDataLog.setText(R.string.show_raw_data);
            btnRawDataLog.setBackgroundColor(btnBackgroundColor);
        }
    }

    private void updateRecordingButtonView() {
        Button btnRecording = findViewById(R.id.btnRecording);
        if (isRecording) {
            btnRecording.setText(R.string.stop_recording);
            btnRecording.setBackgroundColor(getColor(R.color.design_default_color_error));
        } else {
            btnRecording.setText(R.string.start_recording);
            btnRecording.setBackgroundColor(btnBackgroundColor);
        }
    }

    @SuppressWarnings({"BusyWait"})
    public void onClickRecording(View view) {
        consoleView.println("---onClickRecording");
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
                consoleView.println("STOP RECORDING");
                isRecording = false;
            } else {
                consoleView.println("START RECORDING");
                isRecording = true;
                Runnable delayedRunnable = new Runnable() {
                    @Override
                    public void run() {
                        while (isRecording) {
                            waitForData();
                            MainActivity.instance.bufferRead();
                            MainActivity.instance.processInputDataFromSensor();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (consoleView.getSize() > CONSOLE_LINES_LIMIT) {
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
            }
            updateRecordingButtonView();
        } else {
            consoleView.println("CONNECTION IS NOT OPEN");
        }
    }

    private void waitForData() {
        try {
            Thread.sleep(BUFFER_TIME_OUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onClickClearConsole(View view) {
        consoleView.clear();
        consoleView.println("---onClickClearConsole");
        consoleView.println("CONSOLE CLEARED");
    }
}