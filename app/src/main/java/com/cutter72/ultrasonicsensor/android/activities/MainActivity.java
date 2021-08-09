package com.cutter72.ultrasonicsensor.android.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
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
import com.cutter72.ultrasonicsensor.sensor.activists.DataFilterImpl;
import com.cutter72.ultrasonicsensor.sensor.activists.DataListener;
import com.cutter72.ultrasonicsensor.sensor.activists.DataListenerImpl;
import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrierImpl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;

import static com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl.NO_SIGNAL_COUNTER_RESET_VALUE;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_SAF = 1234;
    private static final int IMPACTS_DEFAULT = 0;

    //states
    private boolean isRecording = false;
    private boolean isRawDataLogEnabled = false;

    //layout
    private static final int DEFAULT_INTERVAL_MILLIS = 100;
    private static final double[] minDifferenceValues =
            new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1};
    private static final double[] filterDeviationValues =
            new double[]{0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2};
    @ColorInt
    private int defaultBtnBackgroundColor;
    private ConsoleViewLogger log;
    private ConsoleView consoleView;
    private NumberPicker minDifferenceNumberPicker;
    private NumberPicker filterDeviationNumberPicker;
    private SeekBar minIntervalSeekBar;
    private TextView minIntervalTextValue;

    //sensor data listener
    private DataListener dataListener;
    //data storage
    private SensorDataCarrier recordedSensorData;
    private SensorDataCarrier filteredSensorData;


    //count impacts
    private int minDifferencePickerIndex;
    private int filterDeviationPickerIndex;
    private int intervalMillis; //50ms => 20 impacts / second
    private int impacts;
    private long previousImpactTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeLayout();
        checkPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        isRecording = false;
    }

    private void initializeLayout() {
        recordedSensorData = new SensorDataCarrierImpl();
        filteredSensorData = new SensorDataCarrierImpl();
        isRawDataLogEnabled = false;
        previousImpactTimestamp = 0;
        intervalMillis = DEFAULT_INTERVAL_MILLIS; //50ms => 20 impacts / second
        impacts = IMPACTS_DEFAULT;
        initializeLogger();
        initializeSeekBar();
        initializeRecordingBtn();
        initializeNumberPickers();
        initializeSensorDataListener();
    }

    private void initializeLogger() {
        consoleView = new ConsoleViewImpl(findViewById(R.id.linearLayout), findViewById(R.id.scrollView));
        log = ConsoleViewLoggerImpl.initializeLogger(this, consoleView);
        log.i(TAG, "CONSOLE VIEW CREATED");
    }

    private void initializeSeekBar() {
        minIntervalTextValue = findViewById(R.id.minIntervalTextValue);
        minIntervalSeekBar = findViewById(R.id.minIntervalSeekBar);
        minIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minIntervalTextValue.setText(String.valueOf(convertToMillis(seekBar)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                intervalMillis = convertToMillis(seekBar);
                updateIntervalSeekBarLabel();
            }
        });
        updateIntervalSeekBarView();
        updateIntervalSeekBarLabel();
    }

    private int convertToMillis(SeekBar seekBar) {
        return seekBar.getProgress() * 50 + 50;
    }

    private void updateIntervalSeekBarView() {
        minIntervalSeekBar.setProgress(convertToSeekBarProgressValue());
    }

    private int convertToSeekBarProgressValue() {
        return (intervalMillis - 50) / 50;
    }

    private void updateIntervalSeekBarLabel() {
        minIntervalTextValue.setText(String.valueOf(intervalMillis));
    }

    private void initializeRecordingBtn() {
        Button btnRecording = findViewById(R.id.btnRecording);
        Drawable btnBackgroundDrawable = btnRecording.getBackground();
        defaultBtnBackgroundColor = btnRecording.getBackgroundTintList()
                .getColorForState(btnBackgroundDrawable.getState(), R.color.purple_500);
    }

    private void initializeNumberPickers() {
        minDifferencePickerIndex = getDefaultMinDiffPickerIndex();
        filterDeviationPickerIndex = getDefaultFilterDeviationPickerIndex();
        minDifferenceNumberPicker = findAndPreparePicker(R.id.minDifferencePicker,
                minDifferenceValues,
                (picker, oldVal, newVal) -> setMinDiffValue(newVal));
        filterDeviationNumberPicker = findAndPreparePicker(R.id.filterDeviationPicker,
                filterDeviationValues,
                (picker, oldVal, newVal) -> setFilterValue(newVal));
        updateMinDiffNumberPickerView();
        updateFilterDeviationNumberPickerView();
    }

    private int getDefaultMinDiffPickerIndex() {
        return minDifferenceValues.length / 2;
    }

    private int getDefaultFilterDeviationPickerIndex() {
        return filterDeviationValues.length / 2;
    }

    private NumberPicker findAndPreparePicker(@IdRes int resId, double[] pickerValues,
                                              NumberPicker.OnValueChangeListener onValueChangeListener) {
        NumberPicker numberPicker = findViewById(resId);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(pickerValues.length - 1);
        numberPicker.setDisplayedValues(transformToStringArray(pickerValues));
        numberPicker.setOnValueChangedListener(onValueChangeListener);
        numberPicker.setWrapSelectorWheel(false);
        return numberPicker;
    }

    private String[] transformToStringArray(double[] doubleTab) {
        String[] result = new String[doubleTab.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = String.valueOf(doubleTab[i]);
        }
        return result;
    }

    private void setMinDiffValue(int newIndex) {
        minDifferencePickerIndex = newIndex;
        if (filterDeviationPickerIndex < newIndex) {
            filterDeviationPickerIndex = newIndex;
        }
        updateFilterDeviationNumberPickerView();
    }

    private void setFilterValue(int newIndex) {
        filterDeviationPickerIndex = newIndex;
        if (minDifferencePickerIndex > newIndex) {
            minDifferencePickerIndex = newIndex;
        }
        updateMinDiffNumberPickerView();
    }

    private void updateMinDiffNumberPickerView() {
        minDifferenceNumberPicker.setValue(minDifferencePickerIndex);
    }

    private void updateFilterDeviationNumberPickerView() {
        filterDeviationNumberPicker.setValue(filterDeviationPickerIndex);
    }

    private void initializeSensorDataListener() {
        SensorConnection sensorConnection = new SensorConnectionImpl((UsbManager) getSystemService(USB_SERVICE));
        dataListener = new DataListenerImpl(sensorConnection, data -> {
            if (isRecording) {
                recordedSensorData.addData(data);
                SensorDataCarrier filteredData = new DataFilterImpl()
                        .filterByMedian(data, filterDeviationValues[filterDeviationPickerIndex]);
                filteredSensorData.addData(filteredData);
            }
            if (data.size() > 0) {
                runOnUiThread(() -> printMeasurements(data));
            } else {
                printNoSignalInfo();
            }
        });
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

    private void checkPermissions() {
        if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private boolean checkPermission(String manifestPermission) {
        return ActivityCompat.checkSelfPermission(this,
                manifestPermission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String manifestPermission) {
        Intent intent = new Intent(this, RequestPermissionsActivity.class);
        intent.putExtra(RequestPermissionsActivity.PERMISSION_TYPE, manifestPermission);
        startActivity(intent);
    }

    public void onClickOpenConnection(View view) {
        if (dataListener.isListening()) {
            log.i(TAG, "CONNECTION OPEN");
            dataListener.stopListening();
        } else {
            log.i(TAG, "CONNECTION CLOSED");
            dataListener.startListening();
        }
        updateConnectionButtonView();
    }

    public void onClickRecording(View view) {
        log.v(TAG, "---onClickRecording");
        isRecording = !isRecording;
        if (isRecording) {
            log.i(TAG, "RECORDING START");
        } else {
            log.i(TAG, "RECORDING STOP");
        }
        updateRecordingBtn();
    }

    public void onClickClearConsole(View view) {
        consoleView.clear();
        log.v(TAG, "---onClickClearConsole");
    }

    public void onClickRawDataLog(View view) {
        log.v(TAG, "---onClickRawDataShow");
        isRawDataLogEnabled = !isRawDataLogEnabled;
        updateRawDataLogBtn();
    }

    public void onClickReset(View view) {
        consoleView.clear();
        log.v(TAG, "---onClickReset");
        dataListener.stopListening();
        recordedSensorData.clear();
        isRawDataLogEnabled = false;
        isRecording = false;
        //count impacts
        intervalMillis = DEFAULT_INTERVAL_MILLIS;
        minDifferencePickerIndex = getDefaultMinDiffPickerIndex();
        filterDeviationPickerIndex = getDefaultFilterDeviationPickerIndex();
        impacts = 0;
        previousImpactTimestamp = 0;
        Measurement.resetId();
        updateImpactsCounterView();
        updateIntervalSeekBarView();
        updateFilterDeviationNumberPickerView();
        updateMinDiffNumberPickerView();
        updateMeasurementCounterView();
        updateRecordingBtn();
        updateRawDataLogBtn();
        log.i(TAG, "DATA CLEARED");
    }

    private void updateConnectionButtonView() {
        Button btnOpenConnection = findViewById(R.id.openConnection);
        if (dataListener.isListening()) {
            turnRedAndSetText(btnOpenConnection, R.string.close_connection);
        } else {
            turnBackToDefaultColor(btnOpenConnection, R.string.open_connection);
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
            turnRedAndSetText(btnRawDataLog, R.string.hide_raw_data);
        } else {
            log.i(TAG, "RAW DATA SHOW DISABLED");
            turnBackToDefaultColor(btnRawDataLog, R.string.show_raw_data);
        }
    }

    private void updateRecordingBtn() {
        Button btnRecording = findViewById(R.id.btnRecording);
        if (isRecording) {
            turnRedAndSetText(btnRecording, R.string.stop_recording);
        } else {
            turnBackToDefaultColor(btnRecording, R.string.start_recording);
        }
    }

    private void turnRedAndSetText(Button btn, @StringRes int stringRes) {
        btn.setText(stringRes);
        btn.setBackgroundColor(getColor(R.color.design_default_color_error));
    }

    private void turnBackToDefaultColor(Button btn, @StringRes int stringRes) {
        btn.setText(stringRes);
        btn.setBackgroundColor(defaultBtnBackgroundColor);
    }

    public void onClickSaveDataToCsv(View view) {
        if (recordedSensorData.size() == 0) {
            log.i(TAG, "NO MEASUREMENTS RECORDED. DATA NOT SAVED.");
        } else {
            if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                actionCreateDocument();
            } else {
                requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void actionCreateDocument() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("text/plain")
                .addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_SAF);
    }

    private void createDataFile() {
        FilesManager filesManager = new FilesManagerImpl();
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/UltrasonicSensor");
        filesManager.prepareDirectory(directory.getAbsolutePath());
        File outputFile = new File(directory.getAbsolutePath() + File.separator + String.format("%sImpacts%sMmnts%sInterval%sMinDiff%sFilter.csv",
                impacts,
                recordedSensorData.size(),
                intervalMillis,
                minDifferenceValues[minDifferencePickerIndex],
                filterDeviationValues[filterDeviationPickerIndex]));
        filesManager.writeToFile(outputFile, prepareCsvDataContent());
        log.i(TAG, String.format("MEASUREMENTS DATA EXPORTED TO: %s", outputFile.getAbsolutePath()));
    }

    @NonNull
    private String prepareCsvDataContent() {
        StringBuilder sb = new StringBuilder();
        for (Measurement measurement : recordedSensorData.getRawMeasurements()) {
            sb.append(String.format(Locale.getDefault(), "%d,%.2f,%d%n",
                    measurement.getId(),
                    measurement.getDistanceCentimeters(),
                    measurement.getTime().getTime()
            ));
        }
        return sb.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_SAF) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                log.e(TAG, "Uri from result: " + uri.toString());
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    outputStream.write(prepareCsvDataContent().getBytes());
                    log.i(TAG, String.format("MEASUREMENTS DATA EXPORTED TO: %s", uri.getPath()));
                } catch (IOException e) {
                    log.logException(TAG, e);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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