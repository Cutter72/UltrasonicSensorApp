package com.cutter72.ultrasonicsensor.android.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.MimeTypeMap;
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
import com.cutter72.ultrasonicsensor.sensor.activists.DataDecoderImpl;
import com.cutter72.ultrasonicsensor.sensor.activists.DataFilterImpl;
import com.cutter72.ultrasonicsensor.sensor.activists.DataListener;
import com.cutter72.ultrasonicsensor.sensor.activists.DataListenerImpl;
import com.cutter72.ultrasonicsensor.sensor.activists.ImpactCounterImpl;
import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrierImpl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl.NO_SIGNAL_COUNTER_RESET_VALUE;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ConsoleViewLogger log;
    private static final int REQUEST_SAF = 1234;
    public static final int MEASUREMENTS_IN_ONE_LINE = 18;

    //states
    private boolean isRecording;
    private boolean isRawDataLogEnabled;

    //layout
    private static final int DEFAULT_INTERVAL_MILLIS = 100;
    private static final double[] MIN_DIFFERENCE_VALUES =
            new double[]{0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7,
                    0.75, 0.8, 0.85, 0.9, 0.95, 1.0, 1.05, 1.1};
    private static final double[] FILTER_DEVIATION_VALUES =
            new double[]{0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8,
                    0.85, 0.9, 0.95, 1.0, 1.05, 1.1, 1.15, 1.2};
    @ColorInt
    private int defaultBtnBackgroundColor;
    private int measurementsReceivedCounter;
    private int filteredOutMeasurementsCounter;
    private ConsoleView consoleView;
    private NumberPicker minDifferenceNumberPicker;
    private NumberPicker filterDeviationNumberPicker;
    private SeekBar minIntervalSeekBar;
    private TextView minIntervalTextValue;

    //sensor data listener
    private DataListener dataListener;
    //data storage
    private SensorDataCarrier recordedMeasurements;


    //filters
    private int minDifferencePickerIndex;
    private int filterDeviationPickerIndex;
    private int minTimeIntervalMillis; //50ms => 20 impacts / second
    //impacts found
    private int impactsCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeActivity();
        initializeBroadcastReceiver();
        checkPermissions();
    }

    private void initializeActivity() {
        initializeData();
        initializeLayout();
        updateLayout();
    }

    private void initializeLayout() {
        initializeDefaultBtnColor();
        initializeSeekBar();
        initializeNumberPickers();
    }

    private void initializeDefaultBtnColor() {
        Button btnRecording = findViewById(R.id.btnRecording);
        Drawable btnBackgroundDrawable = btnRecording.getBackground();
        defaultBtnBackgroundColor = btnRecording.getBackgroundTintList()
                .getColorForState(btnBackgroundDrawable.getState(), R.color.purple_500);
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
                minTimeIntervalMillis = convertToMillis(seekBar);
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
        return (minTimeIntervalMillis - 50) / 50;
    }

    private void updateIntervalSeekBarLabel() {
        minIntervalTextValue.setText(String.valueOf(minTimeIntervalMillis));
    }

    private void initializeNumberPickers() {
        minDifferencePickerIndex = getDefaultMinDiffPickerIndex();
        filterDeviationPickerIndex = getDefaultFilterDeviationPickerIndex();
        minDifferenceNumberPicker = findAndPreparePicker(R.id.minDifferencePicker,
                MIN_DIFFERENCE_VALUES,
                (picker, oldVal, newVal) -> setMinDiffValue(newVal));
        filterDeviationNumberPicker = findAndPreparePicker(R.id.filterDeviationPicker,
                FILTER_DEVIATION_VALUES,
                (picker, oldVal, newVal) -> setFilterValue(newVal));
        updateMinDiffNumberPickerView();
        updateFilterDeviationNumberPickerView();
    }

    private int getDefaultMinDiffPickerIndex() {
        return 0;
    }

    private int getDefaultFilterDeviationPickerIndex() {
        return 0;
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
        SensorConnection sensorConnection = new SensorConnectionImpl((UsbManager) getSystemService(USB_SERVICE), log);
        dataListener = new DataListenerImpl(sensorConnection, data -> {
            int dataSize = data.size();
            boolean isDataNotEmpty = dataSize > 0;
            if (isDataNotEmpty) {
                measurementsReceivedCounter += dataSize;
                SensorDataCarrier filteredData = filterByMedian(data);
                filteredOutMeasurementsCounter += dataSize - filteredData.size();
                if (isRecording) {
                    recordedMeasurements.addData(filteredData);
                }
                impactsCounter += findImpacts(filteredData);
                runOnUiThread(() -> updateUiWithNewData(filteredData));
            } else {
                printNoSignalInfo();
            }
        });
    }

    @NonNull
    private SensorDataCarrier filterByMedian(SensorDataCarrier data) {
        return new DataFilterImpl()
                .filterByMedian(data, getFilterDeviationValue());
    }

    private int findImpacts(SensorDataCarrier filteredData) {
        return new ImpactCounterImpl()
                .findImpacts(filteredData.getRawMeasurements(),
                        filteredData.getRawMeasurements().size(),
                        minTimeIntervalMillis,
                        getMinDifferenceValue());
    }

    private void updateUiWithNewData(SensorDataCarrier filteredData) {
        printMeasurements(filteredData);
        updateFilteredOutMeasurementCounterView();
        updateImpactsCounterView();
        updateMeasurementCounterView();
        updateRecordedMeasurementCounterView();
    }

    private void printMeasurements(SensorDataCarrier data) {
        if (isRawDataLogEnabled) {
            log.d(TAG, Arrays.toString(data.getRawData()));
        }
        log.i(TAG, Arrays.toString(data.getRawMeasurements().toArray()));
    }

    private void printMeasurements(List<Measurement> measurements) {
        log.i(TAG, Arrays.toString(measurements.toArray()));
    }

    private void printNoSignalInfo() {
        if (SensorConnectionImpl.noSignalCounter == NO_SIGNAL_COUNTER_RESET_VALUE) {
            log.w(TAG, "NO SIGNAL");
        }
    }

    private void initializeBroadcastReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    log.i(TAG, "USB DEVICE ATTACHED");
                }
                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    log.i(TAG, "USB DEVICE DETACHED");
                }
            }
        };
        IntentFilter usbDeviceAttached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(receiver, usbDeviceAttached);
        IntentFilter usbDeviceDetached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(receiver, usbDeviceDetached);
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
        toggleOpenConnectionBtn();
    }

    private void toggleOpenConnectionBtn() {
        if (dataListener.isListening()) {
            dataListener.stopListening();
        } else {
            dataListener.startListening();
        }
        updateOpenConnectionBtnView();
    }

    public void onClickRecording(View view) {
        log.v(TAG, "---onClickRecording");
        toggleRecordingBtn();
    }

    private void toggleRecordingBtn() {
        isRecording = !isRecording;
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
        initializeData();
        updateLayout();
        log.i(TAG, "DATA CLEARED");
    }

    private void initializeData() {
        isRawDataLogEnabled = false;
        isRecording = false;
        impactsCounter = 0;
        measurementsReceivedCounter = 0;
        filteredOutMeasurementsCounter = 0;
        recordedMeasurements = new SensorDataCarrierImpl(new DataDecoderImpl());
        //count impacts
        minTimeIntervalMillis = DEFAULT_INTERVAL_MILLIS;
        minDifferencePickerIndex = getDefaultMinDiffPickerIndex();
        filterDeviationPickerIndex = getDefaultFilterDeviationPickerIndex();
        Measurement.resetId();
        initializeConsoleViewAndLogger();
        if (dataListener != null) {
            dataListener.stopListening();
        } else {
            initializeSensorDataListener();
        }
    }

    private void initializeConsoleViewAndLogger() {
        consoleView = new ConsoleViewImpl(findViewById(R.id.linearLayout), findViewById(R.id.scrollView));
        log = new ConsoleViewLoggerImpl(this, consoleView);
        log.i(TAG, "CONSOLE VIEW CREATED");
    }

    private void updateLayout() {
        updateFilterDeviationNumberPickerView();
        updateFilteredOutMeasurementCounterView();
        updateImpactsCounterView();
        updateIntervalSeekBarView();
        updateMeasurementCounterView();
        updateMinDiffNumberPickerView();
        updateOpenConnectionBtnView();
        updateRawDataLogBtn();
        updateRecordedMeasurementCounterView();
        updateRecordingBtn();

    }

    private void updateOpenConnectionBtnView() {
        Button btnOpenConnection = findViewById(R.id.openConnection);
        if (dataListener.isListening()) {
            turnRedAndSetText(btnOpenConnection, R.string.close_connection);
            log.i(TAG, "CONNECTION OPEN");
        } else {
            turnBackToDefaultColor(btnOpenConnection, R.string.open_connection);
            log.i(TAG, "CONNECTION CLOSED");
        }
    }

    private void updateImpactsCounterView() {
        ((TextView) findViewById(R.id.impactsCounter)).setText(String.valueOf(impactsCounter));
    }

    private void updateMeasurementCounterView() {
        ((TextView) findViewById(R.id.measurementsCounter)).setText(String.valueOf(measurementsReceivedCounter));
    }

    private void updateRecordedMeasurementCounterView() {
        ((TextView) findViewById(R.id.recordedMeasurementsCounter)).setText(String.valueOf(recordedMeasurements.size()));
    }

    private void updateFilteredOutMeasurementCounterView() {
        ((TextView) findViewById(R.id.filteredOutMeasurementsCounter)).setText(String.valueOf(filteredOutMeasurementsCounter));
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
            log.i(TAG, "RECORDING START");
        } else {
            turnBackToDefaultColor(btnRecording, R.string.start_recording);
            log.i(TAG, "RECORDING STOP");
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
        if (isRecording) {
            toggleRecordingBtn();
        }
        if (recordedMeasurements.size() == 0) {
            log.i(TAG, "NO MEASUREMENTS RECORDED. DATA NOT SAVED.");
        } else {
            if (dataListener.isListening()) {
                toggleOpenConnectionBtn();
            }
            if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                actionCreateDocument();
            } else {
                requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void actionCreateDocument() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType(getMimeTypeFromExtension("csv")) // MIME types -> https://en.wikipedia.org/wiki/Media_type
                .addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_SAF);
    }

    private String getMimeTypeFromExtension(String fileExtension) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
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

    @NonNull
    private String prepareCsvDataContent() {
        StringBuilder sb = new StringBuilder();
        for (Measurement measurement : recordedMeasurements.getRawMeasurements()) {
            sb.append(String.format(Locale.ROOT, "%d,%.2f,%d%n",
                    measurement.getId(),
                    measurement.getDistanceCentimeters(),
                    measurement.getDate().getTime()
            ));
        }
        return sb.toString();
    }

    private void createDataFile() {
        FilesManager filesManager = new FilesManagerImpl();
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/UltrasonicSensor");
        filesManager.prepareDirectory(directory.getAbsolutePath());
        File outputFile = new File(directory.getAbsolutePath() + File.separator + String.format("%sImpacts%sMmnts%sInterval%sMinDiff%sFilter.csv",
                impactsCounter,
                recordedMeasurements.size(),
                minTimeIntervalMillis,
                getMinDifferenceValue(),
                getFilterDeviationValue()));
        filesManager.writeToFile(outputFile, prepareCsvDataContent());
        log.i(TAG, String.format("MEASUREMENTS DATA EXPORTED TO: %s", outputFile.getAbsolutePath()));
    }

    private double getMinDifferenceValue() {
        return MIN_DIFFERENCE_VALUES[minDifferencePickerIndex];
    }

    private double getFilterDeviationValue() {
        return FILTER_DEVIATION_VALUES[filterDeviationPickerIndex];
    }
}