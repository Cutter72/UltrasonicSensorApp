package com.cutter72.ultrasonicsensor.sensor.activists;

public interface DataListener {
    boolean startListening();

    boolean isListening();

    void stopListening();
}
