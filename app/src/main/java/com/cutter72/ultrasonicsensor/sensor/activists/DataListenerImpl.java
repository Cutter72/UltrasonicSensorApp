package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.balsikandar.crashreporter.CrashReporter;
import com.cutter72.ultrasonicsensor.sensor.SensorConnection;
import com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DataListenerImpl implements DataListener {
    private final SensorConnection sensorConnection;
    private final ExecutorService executorService;
    private final DataCallback dataCallback;
    private boolean isListening;

    public DataListenerImpl(@NonNull SensorConnection sensorConnection, @NonNull DataCallback dataCallback) {
        this.sensorConnection = sensorConnection;
        this.dataCallback = dataCallback;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean startListening() {
        if (sensorConnection.open()) {
            isListening = true;
            executorService.submit(() -> {
                sensorConnection.clearHardwareInputOutputBuffers();
                while (isListening) {
                    if (waitForData()) {
                        try {
                            SensorDataCarrier receivedData = sensorConnection.readData();
                            new MeasurementsTimeApproximatorImpl()
                                    .approximate(receivedData.getRawMeasurements(),
                                            SensorConnectionImpl.DEFAULT_BUFFER_TIME_OUT_MILLIS);
                            dataCallback.onDataReceive(receivedData);
                        } catch (Exception e) {
                            e.printStackTrace();
                            CrashReporter.logException(e);
                            stopListening();
                        }
                    } else {
                        stopListening();
                    }
                }
            });
            return true;
        } else {
            System.out.println("openConnectionFailure");
        }
        return false;
    }

    private boolean waitForData() {
        try {
            TimeUnit.MILLISECONDS.sleep(SensorConnectionImpl.DEFAULT_BUFFER_TIME_OUT_MILLIS);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            CrashReporter.logException(e);
            return false;
        }
    }

    @Override
    public boolean isListening() {
        return isListening;
    }

    @Override
    public void stopListening() {
        isListening = false;
        sensorConnection.close();
    }
}
