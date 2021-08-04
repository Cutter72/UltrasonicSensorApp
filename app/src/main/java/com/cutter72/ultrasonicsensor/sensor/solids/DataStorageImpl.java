package com.cutter72.ultrasonicsensor.sensor.solids;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.activists.DataDecoder;
import com.cutter72.ultrasonicsensor.sensor.activists.DataDecoderImpl;

import java.util.ArrayList;
import java.util.List;

public class DataStorageImpl implements DataStorage {
    private final List<byte[]> rawData;
    private final List<Measurement> rawMeasurements;

    public DataStorageImpl() {
        this.rawData = new ArrayList<>();
        this.rawMeasurements = new ArrayList<>();
    }

    @Override
    public List<Measurement> addRawData(@NonNull byte[] rawData) {
        this.rawData.add(rawData);
        DataDecoder dataDecoder = new DataDecoderImpl();
        List<Measurement> rawMeasurementsChunk = dataDecoder.decodeDataFromSensor(rawData);
        this.rawMeasurements.addAll(rawMeasurementsChunk);
        return rawMeasurementsChunk;
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
        int size = this.rawMeasurements.size();
        if (size > howMany) {
            return this.rawMeasurements.subList(size - howMany, size);
        } else {
            return this.rawMeasurements;
        }
    }
}
