package com.cutter72.ultrasonicsensor.android.activities;

import android.Manifest;
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

import com.cutter72.ultrasonicsensor.R;
import com.cutter72.ultrasonicsensor.android.other.ConsoleViewImpl;
import com.cutter72.ultrasonicsensor.android.other.ConsoleViewLogger;
import com.cutter72.ultrasonicsensor.android.other.ConsoleViewLoggerImpl;
import com.cutter72.ultrasonicsensor.files.FilesManager;
import com.cutter72.ultrasonicsensor.files.FilesManagerImpl;
import com.cutter72.ultrasonicsensor.sensor.SensorConnection;
import com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl;
import com.cutter72.ultrasonicsensor.sensor.activists.DataCallback;
import com.cutter72.ultrasonicsensor.sensor.activists.DataListener;
import com.cutter72.ultrasonicsensor.sensor.activists.DataListenerImpl;
import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrierImpl;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl.NO_SIGNAL_COUNTER_RESET_VALUE;

@SuppressWarnings({"Convert2Lambda"})
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double CENTIMETERS_UNIT_FACTOR = 0.00859536; //value in centimeters from ToughSonic Sensor 12 data sheet
    public static MainActivity instance;
    public static boolean isRecording = false;
    private ConsoleViewLogger log;

    //RS232 connection
    private SensorConnection sensorConnection;
    private SensorDataCarrier sensorDataCarrier;
    private DataListener dataListener;
    //todo create SensorDataRecorder
    //todo add logger
    public static boolean isOpened = false;
    private List<UsbSerialDriver> availableDrivers;
    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private UsbSerialPort port;

    //layout
    private final int SEEKBAR_MAX_VALUE = 19;
    //todo make consol view with a global access from all classes
    private ConsoleViewImpl consoleView;
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
        initializeLogger();
        sensorConnection = new SensorConnectionImpl((UsbManager) getSystemService(USB_SERVICE));
        sensorDataCarrier = new SensorDataCarrierImpl();
        dataListener = new DataListenerImpl(sensorConnection, new DataCallback() {
            @Override
            public void onDataReceive(SensorDataCarrier data) {
                if (isRecording) {
                    sensorDataCarrier.addData(data);
                }
                if (data.size() > 0) {
                    runOnUiThread(() -> printMeasurements(data));
                } else {
                    printNoSignalInfo();
                }
            }
        });
        isRawDataLogEnabled = false;
        rawSensorUnitsBuffer = Collections.synchronizedList(new LinkedList<>());
        previousImpactTimestamp = 0;
        minDifference = MIN_DIFFERENCE_DEFAULT;
        avgMeasurements = AVG_MEASUREMENTS_DEFAULT;
        minTimeIntervalBetweenImpactMillis = MIN_INTERVAL_BETWEEN_IMPACTS_MILLIS_DEFAULT; //50ms => 20 impacts / second
        impacts = IMPACTS_DEFAULT;
    }

    private void printMeasurements(SensorDataCarrier data) {
        if (isRawDataLogEnabled) {
            log.d(TAG, Arrays.toString(data.getRawData()));
        }
        log.i(TAG, Arrays.toString(data.getRawMeasurements().toArray()));
    }

    private void printNoSignalInfo() {
        if (SensorConnectionImpl.noSignalCounter == NO_SIGNAL_COUNTER_RESET_VALUE) {
            log.w(TAG, "NO SIGNAL");
        }
    }

    private void initializeLayout() {
        instance = this;
        Button btnRecording = findViewById(R.id.btnRecording);
        Drawable btnBackgroundDrawable = btnRecording.getBackground();
        btnBackgroundColor = btnRecording.getBackgroundTintList().getColorForState(btnBackgroundDrawable.getState(), R.color.purple_500);
        updateIntervalValueTextView();
        initializeSeekBar();
        initializeNumberPickers();
    }

    private void initializeLogger() {
        consoleView = new ConsoleViewImpl(findViewById(R.id.linearLayout), findViewById(R.id.scrollView));
        log = ConsoleViewLoggerImpl.initializeLogger(this, consoleView);
        log.i(TAG, "Console view created.");
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
        log.i(TAG, "---onClickOpenConnection");
        if (dataListener.isListening()) {
            dataListener.stopListening();
        } else {
            dataListener.startListening();
        }
        updateConnectionButtonView();
    }

    public void onClickSaveDataToCsv(View view) {
        if (allMeasurements.size() == 0) {
            log.i(TAG, "NO MEASUREMENTS RECORDED. DATA NOT SAVED.");
        } else {
            requestPermissionsForWrite();
        }
    }

    private void createDataFile() {
        //todo think how to save all data
        FilesManager filesManager = new FilesManagerImpl();
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/UltrasonicSensor");
        filesManager.prepareDirectory(directory.getAbsolutePath());
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
            sb.append(measurement.getDistanceCentimeters());
            sb.append("\n");
        }
        filesManager.writeToFile(outputFile, sb.toString());
        log.i(TAG, String.format("MEASUREMENTS DATA EXPORTED TO: %s", outputFile.getAbsolutePath()));
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

    private void updateConnectionButtonView() {
        Button btnOpenConnection = findViewById(R.id.openConnection);
        if (dataListener.isListening()) {
            btnOpenConnection.setText(R.string.close_connection);
            btnOpenConnection.setBackgroundColor(getColor(R.color.design_default_color_error));
        } else {
            btnOpenConnection.setText(R.string.open_connection);
            btnOpenConnection.setBackgroundColor(btnBackgroundColor);
        }
    }

    private boolean isImpactFound() {
        if (allNonZeroMeasurements.size() > avgMeasurements) {
            double sum = 0;
            for (int i = allNonZeroMeasurements.size() - avgMeasurements - 1; i < allNonZeroMeasurements.size() - 1; i++) {
                sum += allNonZeroMeasurements.get(i).getDistanceCentimeters();
            }
            double averageFromPreviousXMeasurements = sum / avgMeasurements;
            double differenceToCheck = averageFromPreviousXMeasurements - allNonZeroMeasurements.get(allNonZeroMeasurements.size() - 1).getDistanceCentimeters();
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

    private void updateMeasurementCounterView() {
        ((TextView) findViewById(R.id.measurementsCounter)).setText(String.valueOf(allNonZeroMeasurements.size()));
    }

    private void printLatest18MeasurementsAndUpdateCounter() {
        updateMeasurementCounterView();
        if (isRawDataLogEnabled) {
            log.i(TAG, "allMeasurements.size(): " + allNonZeroMeasurements.size());
        }
        log.i(TAG, "");
        for (int i = allNonZeroMeasurements.size() - MEASUREMENTS_IN_ONE_LINE; i < allNonZeroMeasurements.size(); i++) {
            consoleView.print(allNonZeroMeasurements.get(i).getDistanceCentimeters() + ", ");
        }
    }

    public void onClickRawDataLog(View view) {
        log.i(TAG, "---onClickRawDataShow");
        isRawDataLogEnabled = !isRawDataLogEnabled;
        updateRawDataLogBtn();
    }

    public void onClickReset(View view) {
        consoleView.clear();
        log.i(TAG, "---onClickReset");
        dataListener.stopListening();
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
        updateRawDataLogBtn();
        log.i(TAG, "DATA CLEARED");
    }

    private void updateRawDataLogBtn() {
        Button btnRawDataLog = findViewById(R.id.btnRawDataLog);
        if (isRawDataLogEnabled) {
            log.i(TAG, "RAW DATA SHOW ENABLED");
            btnRawDataLog.setText(R.string.hide_raw_data);
            btnRawDataLog.setBackgroundColor(getColor(R.color.design_default_color_error));
        } else {
            log.i(TAG, "RAW DATA SHOW DISABLED");
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

    public void onClickRecording(View view) {
        log.i(TAG, "---onClickRecording");
        //todo impl
    }

    public void onClickClearConsole(View view) {
        consoleView.clear();
        log.i(TAG, "---onClickClearConsole");
        log.i(TAG, "CONSOLE CLEARED");
    }
}