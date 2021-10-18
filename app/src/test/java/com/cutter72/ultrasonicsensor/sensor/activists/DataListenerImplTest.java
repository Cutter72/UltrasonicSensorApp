package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.SensorConnection;
import com.cutter72.ultrasonicsensor.sensor.SensorConnectionImpl;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrier;
import com.cutter72.ultrasonicsensor.sensor.solids.SensorDataCarrierImpl;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DataListenerImplTest {
    private DataListener dataListenerConnected;
    private DataListener dataListenerNotConnected;
    private List<SensorDataCarrier> dataReceived;

    @Before
    public void setUp() {
        dataReceived = new ArrayList<>();
        SensorConnection sensorNotConnected = getNotConnectedSensor();
        SensorConnection sensorConnected = getConnectedSensor();
        DataCallback dataCallback = data -> dataReceived.add(data);
        dataListenerNotConnected = new DataListenerImpl(sensorNotConnected, dataCallback);
        dataListenerConnected = new DataListenerImpl(sensorConnected, dataCallback);
    }

    @NonNull
    private SensorConnection getConnectedSensor() {
        return new SensorConnection() {
            @Override
            public boolean open() {
                return true;
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @NonNull
            @Override
            public SensorDataCarrier readData() {
                return new SensorDataCarrierImpl(new DataDecoderImpl());
            }

            @NonNull
            @Override
            public SensorDataCarrier readData(@NonNull byte[] buffer) {
                return new SensorDataCarrierImpl(new DataDecoderImpl());
            }

            @Override
            public void close() {

            }

            @Override
            public boolean clearHardwareInputOutputBuffers() {
                return true;
            }
        };
    }

    @NonNull
    private SensorConnection getNotConnectedSensor() {
        return new SensorConnection() {
            @Override
            public boolean open() {
                return false;
            }

            @Override
            public boolean isOpen() {
                return false;
            }

            @NonNull
            @Override
            public SensorDataCarrier readData() {
                return new SensorDataCarrierImpl(new DataDecoderImpl());
            }

            @NonNull
            @Override
            public SensorDataCarrier readData(@NonNull byte[] buffer) {
                return new SensorDataCarrierImpl(new DataDecoderImpl());
            }

            @Override
            public void close() {

            }

            @Override
            public boolean clearHardwareInputOutputBuffers() {
                return false;
            }
        };
    }

    @Test
    public void startListeningNotConnected() {
        assertFalse(dataListenerNotConnected.startListening());
    }

    @Test
    public void isListeningNotConnected() {
        assertFalse(dataListenerNotConnected.isListening());
    }

    @Test
    public void startListeningConnected() {
        assertTrue(dataListenerConnected.startListening());
        assertTrue(dataListenerConnected.isListening());
        assertTrue(waitForData(SensorConnectionImpl.DEFAULT_BUFFER_TIME_OUT_MILLIS / 2));
        System.out.println("dataReceived.size() = " + dataReceived.size());
        assertEquals(0, dataReceived.size());
        assertTrue(waitForData(SensorConnectionImpl.DEFAULT_BUFFER_TIME_OUT_MILLIS));
        System.out.println("dataReceived.size() = " + dataReceived.size());
        assertTrue(dataReceived.size() > 0);
        dataListenerConnected.stopListening();
        assertFalse(dataListenerConnected.isListening());
    }

    private boolean waitForData(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Test
    public void isListeningConnected() {
        assertTrue(dataListenerConnected.startListening());
        assertTrue(dataListenerConnected.isListening());
        dataListenerConnected.stopListening();
        assertFalse(dataListenerConnected.isListening());
    }
}