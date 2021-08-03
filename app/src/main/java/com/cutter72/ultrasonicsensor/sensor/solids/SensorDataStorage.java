package com.cutter72.ultrasonicsensor.sensor.solids;

import com.cutter72.ultrasonicsensor.sensor.activists.SensorDataDecoder;
import com.cutter72.ultrasonicsensor.sensor.activists.SensorDataDecoderImpl;

import java.util.ArrayList;
import java.util.List;

public class SensorDataStorage {
    private final List<byte[]> rawData;
    private final List<Measurement> rawMeasurements;

    public SensorDataStorage() {
        this.rawData = new ArrayList<>();
        this.rawMeasurements = new ArrayList<>();
    }

    public void addRawData(byte[] rawData) {
        this.rawData.add(rawData);
        SensorDataDecoder sensorDataDecoder = new SensorDataDecoderImpl();
        this.rawMeasurements.addAll(sensorDataDecoder.decodeDataFromSensor(rawData));
    }

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

    public List<Measurement> getRawMeasurements() {
        return this.rawMeasurements;
    }

    public List<Measurement> getLastMeasurements(int howMany) {
        int size = this.rawMeasurements.size();
        return this.rawMeasurements.subList(size - howMany, size);
    }
}
