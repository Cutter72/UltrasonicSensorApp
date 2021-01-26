package com.example.ultrasonicsensor;

import java.util.Date;

public class Measurement {
    private final double centimetersDistance;
    private final Date time;

    public Measurement(double centimetersDistance) {
        this.centimetersDistance = centimetersDistance;
        this.time = new Date();
    }

    public double getCentimetersDistance() {
        return centimetersDistance;
    }

    public Date getTime() {
        return time;
    }
}
