package com.cutter72.ultrasonicsensor.sensor.activists;

public interface DataListener {
    void startListening();

    boolean isListening();

    void stopListening();
}
