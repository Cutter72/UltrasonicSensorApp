package com.cutter72.ultrasonicsensor.sensor;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

public class Measurement implements Comparable<Measurement> {
    private final double centimetersDistance;
    private final Date time;

    public Measurement(double centimetersDistance) {
        this.centimetersDistance = centimetersDistance;
        this.time = new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Measurement that = (Measurement) o;
        return Double.compare(that.centimetersDistance, centimetersDistance) == 0 &&
                Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(centimetersDistance, time);
    }

    @NonNull
    @Override
    public String toString() {
        return "" + centimetersDistance;
    }

    public double getCentimetersDistance() {
        return centimetersDistance;
    }

    public Date getTime() {
        return time;
    }

    @Override
    public int compareTo(Measurement measurement) {
        return Double.compare(this.centimetersDistance, measurement.getCentimetersDistance());
    }
}
