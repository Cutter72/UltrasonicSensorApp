package com.cutter72.ultrasonicsensor.android.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
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
import com.cutter72.ultrasonicsensor.android.other.ConsoleView;
import com.cutter72.ultrasonicsensor.android.other.ConsoleViewImpl;
import com.cutter72.ultrasonicsensor.android.other.ConsoleViewLogger;
import com.cutter72.ultrasonicsensor.android.other.ConsoleViewLoggerImpl;
import com.cutter72.ultrasonicsensor.files.FilesManager;
import com.cutter72.ultrasonicsensor.files.FilesManagerImpl;
import com.cutter72.ultrasonicsensor.sensor.SensorConnection;
import com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl;
import com.cutter72.ultrasonicsensor.sensor.activists.DataCallback;
import com.cutter72.ultrasonicsensor.sensor.activists.DataFilterImpl;
import com.cutter72.ultrasonicsensor.sensor.activists.DataListener;
import com.cutter72.ultrasonicsensor.sensor.activists.DataListenerImpl;
import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrierImpl;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

import static com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl.NO_SIGNAL_COUNTER_RESET_VALUE;

@SuppressWarnings({"Convert2Lambda"})
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int DEFAULT_FILTER_PICKER_INDEX = 5;
    private static final int DEFAULT_MIN_DIFF_PICKER_INDEX = 5;
    private ConsoleViewLogger log;
    private boolean isRecording = false;
    private boolean isRawDataLogEnabled = false;

    //layout
    private ConsoleView consoleView;
    private int btnBackgroundColor;

    private DataListener dataListener;
    private SensorDataCarrier recordedSensorData;
    private SensorDataCarrier filteredSensorData;


    //count impacts
    private final int MIN_INTERVAL_BETWEEN_IMPACTS_MILLIS_DEFAULT = 100;
    private final int IMPACTS_DEFAULT = 0;
    private final double[] minDiffValues =
            new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1};
    private final double[] filterValues =
            new double[]{0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2};
    private int minDifferencePickerIndex;
    private int filterValuePickerIndex;
    private int minTimeIntervalBetweenImpactMillis; //50ms => 20 impacts / second
    private int impacts;
    private long previousImpactTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeLayout();
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
        recordedSensorData = new SensorDataCarrierImpl();
        filteredSensorData = new SensorDataCarrierImpl();
        isRawDataLogEnabled = false;
        previousImpactTimestamp = 0;
        minTimeIntervalBetweenImpactMillis = MIN_INTERVAL_BETWEEN_IMPACTS_MILLIS_DEFAULT; //50ms => 20 impacts / second
        impacts = IMPACTS_DEFAULT;
        initializeLogger();
        initializeSeekBar();
        initializeNumberPickers();
        initializeSensorDataListener();
        initializeRecordingBtn();
        updateIntervalSeekBarLabel();
    }

    private void initializeRecordingBtn() {
        Button btnRecording = findViewById(R.id.btnRecording);
        Drawable btnBackgroundDrawable = btnRecording.getBackground();
        btnBackgroundColor = btnRecording.getBackgroundTintList().getColorForState(btnBackgroundDrawable.getState(), R.color.purple_500);
    }

    private void initializeSensorDataListener() {
        SensorConnection sensorConnection = new SensorConnectionImpl((UsbManager) getSystemService(USB_SERVICE));
        dataListener = new DataListenerImpl(sensorConnection, new DataCallback() {
            @Override
            public void onDataReceive(SensorDataCarrier data) {
                if (isRecording) {
                    recordedSensorData.addData(data);
                    SensorDataCarrier filteredData = new DataFilterImpl()
                            .filterByMedian(data, filterValues[filterValuePickerIndex]);
                    filteredSensorData.addData(filteredData);
                }
                if (data.size() > 0) {
                    runOnUiThread(() -> printMeasurements(data));
                } else {
                    printNoSignalInfo();
                }
            }
        });
    }

    private void initializeLogger() {
        consoleView = new ConsoleViewImpl(findViewById(R.id.linearLayout), findViewById(R.id.scrollView));
        log = ConsoleViewLoggerImpl.initializeLogger(this, consoleView);
        log.i(TAG, "Console view created.");
    }

    private void initializeSeekBar() {
        SeekBar minTimeIntervalSeekBar = findViewById(R.id.minTimeIntervalSeekBar);
        updateIntervalSeekBarView();
        minTimeIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((TextView) findViewById(R.id.minTimeInterval)).setText(String.valueOf(convertToMillis(seekBar)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                minTimeIntervalBetweenImpactMillis = convertToMillis(seekBar);
                updateIntervalSeekBarLabel();
            }
        });
    }

    private int convertToMillis(SeekBar seekBar) {
        return seekBar.getProgress() * 50 + 50;
    }

    private void initializeNumberPickers() {
        filterValuePickerIndex = DEFAULT_FILTER_PICKER_INDEX;
        minDifferencePickerIndex = DEFAULT_MIN_DIFF_PICKER_INDEX;
        NumberPicker minDifferencePicker = findViewById(R.id.minDifferencePicker);
        minDifferencePicker.setMinValue(0);
        minDifferencePicker.setMaxValue(minDiffValues.length - 1);
        minDifferencePicker.setDisplayedValues(transformToStringArray(minDiffValues));
        updateMinDiffValuePickerView();
        minDifferencePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setMinDiffValue(newVal);
            }
        });
        NumberPicker filterValuePicker = findViewById(R.id.avgMeasurementsPicker);
        filterValuePicker.setMinValue(0);
        filterValuePicker.setMaxValue(filterValues.length - 1);
        filterValuePicker.setDisplayedValues(transformToStringArray(filterValues));
        updateFilterValuePickerView();
        filterValuePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setFilterValue(newVal);
            }
        });
    }

    private void updateIntervalSeekBarLabel() {
        ((TextView) findViewById(R.id.minTimeInterval)).setText(String.valueOf(minTimeIntervalBetweenImpactMillis));
    }

    private void updateIntervalSeekBarView() {
        ((SeekBar) findViewById(R.id.minTimeIntervalSeekBar)).setProgress(convertToSeekBarProgressValue());
    }

    private int convertToSeekBarProgressValue() {
        return (minTimeIntervalBetweenImpactMillis - 50) / 50;
    }

    private void updateMinDiffValuePickerView() {
        ((NumberPicker) findViewById(R.id.minDifferencePicker)).setValue(minDifferencePickerIndex);
    }

    private void updateFilterValuePickerView() {
        ((NumberPicker) findViewById(R.id.avgMeasurementsPicker)).setValue(filterValuePickerIndex);
    }

    private void setMinDiffValue(int newIndex) {
        minDifferencePickerIndex = newIndex;
        if (filterValuePickerIndex > newIndex) {
            filterValuePickerIndex = newIndex;
        }
    }

    private void setFilterValue(int newIndex) {
        filterValuePickerIndex = newIndex;
        if (minDifferencePickerIndex > newIndex) {
            minDifferencePickerIndex = newIndex;
        }
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
        if (dataListener.isListening()) {
            log.i(TAG, "---onClickCloseConnection");
            dataListener.stopListening();
        } else {
            log.i(TAG, "---onClickOpenConnection");
            dataListener.startListening();
        }
        updateConnectionButtonView();
    }

    public void onClickRecording(View view) {
        log.i(TAG, "---onClickRecording");
        isRecording = !isRecording;
        updateRecordingBtn();
    }

    public void onClickClearConsole(View view) {
        consoleView.clear();
        log.i(TAG, "---onClickClearConsole");
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
        recordedSensorData.clear();
        isRawDataLogEnabled = false;
        //count impacts
        minTimeIntervalBetweenImpactMillis = MIN_INTERVAL_BETWEEN_IMPACTS_MILLIS_DEFAULT;
        impacts = 0;
        previousImpactTimestamp = 0;
        Measurement.nextId = 0;
        updateIntervalSeekBarView();
        updateFilterValuePickerView();
        updateMinDiffValuePickerView();
        updateImpactsCounterView();
        updateMeasurementCounterView();
        updateRecordingBtn();
        updateRawDataLogBtn();
        log.i(TAG, "DATA CLEARED");
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

    private void updateImpactsCounterView() {
        ((TextView) findViewById(R.id.impactsCounter)).setText(String.valueOf(impacts));
    }

    private void updateMeasurementCounterView() {
        ((TextView) findViewById(R.id.measurementsCounter)).setText(String.valueOf(recordedSensorData.size()));
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

    private void updateRecordingBtn() {
        Button btnRecording = findViewById(R.id.btnRecording);
        if (isRecording) {
            btnRecording.setText(R.string.stop_recording);
            btnRecording.setBackgroundColor(getColor(R.color.design_default_color_error));
        } else {
            btnRecording.setText(R.string.start_recording);
            btnRecording.setBackgroundColor(btnBackgroundColor);
        }
    }

    public void onClickSaveDataToCsv(View view) {
        if (recordedSensorData.size() == 0) {
            log.i(TAG, "NO MEASUREMENTS RECORDED. DATA NOT SAVED.");
        } else {
            requestPermissionsForWrite();
        }
    }

    private void createDataFile() {
        FilesManager filesManager = new FilesManagerImpl();
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/UltrasonicSensor");
        filesManager.prepareDirectory(directory.getAbsolutePath());
        File outputFile = new File(directory.getAbsolutePath() + File.separator + String.format("%sImpacts%sMmnts%sInterval%sMinDiff%sFilter.csv",
                impacts,
                recordedSensorData.size(),
                minTimeIntervalBetweenImpactMillis,
                minDiffValues[minDifferencePickerIndex],
                filterValues[filterValuePickerIndex]));
        StringBuilder sb = new StringBuilder();
        for (Measurement measurement : recordedSensorData.getRawMeasurements()) {
            sb.append(String.format(Locale.getDefault(), "%d,%.2f,%d%n",
                    measurement.getId(),
                    measurement.getDistanceCentimeters(),
                    measurement.getTime().getTime()
            ));
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

//    private boolean isImpactFound() {
//        if (recordedSensorData.size() > maxDifference) {
//            double sum = 0;
//            for (int i = filteredSensorData.size() - maxDifference - 1; i < filteredSensorData.size() - 1; i++) {
//                sum += recordedSensorData.get(i).getDistanceCentimeters();
//            }
//            double averageFromPreviousXMeasurements = sum / maxDifference;
//            double differenceToCheck = averageFromPreviousXMeasurements - recordedSensorData.get(recordedSensorData.size() - 1).getDistanceCentimeters();
//            if (differenceToCheck > minDifference) {
//                long currentMillis = System.currentTimeMillis();
//                long timeDifference = currentMillis - previousImpactTimestamp;
//                if (timeDifference >= minTimeIntervalBetweenImpactMillis) {
//                    impacts++;
//                    previousImpactTimestamp = currentMillis;
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
}