package com.cutter72.ultrasonicsensor.sensor.solids;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.activists.DataDecoder;

import java.util.ArrayList;
import java.util.List;

public class SensorDataCarrierImpl implements SensorDataCarrier {
    private final DataDecoder dataDecoder;
    private final List<byte[]> rawData;
    private final List<Measurement> rawMeasurements;

    public SensorDataCarrierImpl(DataDecoder dataDecoder) {
        this.dataDecoder = dataDecoder;
        this.rawData = new ArrayList<>();
        this.rawMeasurements = new ArrayList<>();
    }

    @Override
    public SensorDataCarrier addRawData(@NonNull byte[] rawData) {
        this.rawData.add(rawData);
        this.rawMeasurements.addAll(dataDecoder.decodeDataFromSensor(rawData));
        return this;
    }

    @Override
    public SensorDataCarrier addData(SensorDataCarrier sensorDataCarrier) {
        this.rawData.add(sensorDataCarrier.getRawData());
        this.rawMeasurements.addAll(sensorDataCarrier.getRawMeasurements());
        return this;
    }

    @NonNull
    @Override
    public byte[] getRawData() {
        int summarizedLength = findSummarizedDataLength();
        byte[] outputData = new byte[summarizedLength];
        int index = 0;
        for (byte[] rawDataChunk : rawData) {
            for (byte b : rawDataChunk) {
                outputData[index] = b;
                index++;
            }
        }
        return outputData;
    }

    private int findSummarizedDataLength() {
        int summarizedLength = 0;
        for (byte[] rawDataChunk : this.rawData) {
            summarizedLength += rawDataChunk.length;
        }
        return summarizedLength;
    }

    @NonNull
    @Override
    public List<Measurement> getRawMeasurements() {
        return this.rawMeasurements;
    }

    @NonNull
    @Override
    public List<Measurement> getLastMeasurements(int howMany) {
        int size = size();
        if (size > howMany) {
            return this.rawMeasurements.subList(size - howMany, size);
        } else {
            return this.rawMeasurements;
        }
    }

    @Override
    public int size() {
        return this.rawMeasurements.size();
    }

    @Override
    public Measurement get(int index) {
        return this.rawMeasurements.get(index);
    }

    @Override
    public void clear() {
        rawData.clear();
        rawMeasurements.clear();
    }

    @Override
    public SensorDataCarrier setRawData(List<byte[]> rawData) {
        this.rawData.clear();
        return this;
    }

    @Override
    public SensorDataCarrier setRawData(byte[] rawData) {
        this.rawData.clear();
        this.rawData.add(rawData);
        return this;
    }

    @Override
    public SensorDataCarrier setRawMeasurements(List<Measurement> measurementsToAdd) {
        this.rawMeasurements.clear();
        this.rawMeasurements.addAll(measurementsToAdd);
        return this;
    }
}
