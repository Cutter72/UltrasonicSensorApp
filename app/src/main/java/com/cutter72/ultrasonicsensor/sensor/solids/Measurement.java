package com.cutter72.ultrasonicsensor.sensor.solids;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

public class Measurement implements Comparable<Measurement> {
    public static int nextId = 0;
    private final int id;
    private final double distanceCentimeters;
    private final Date time;

    public Measurement(double distanceCentimeters) {
        this.distanceCentimeters = distanceCentimeters;
        this.time = new Date();
        this.id = nextId;
        nextId++;
    }

    public int getId() {
        return id;
    }

    public double getDistanceCentimeters() {
        return distanceCentimeters;
    }

    public Date getTime() {
        return time;
    }

    @Override
    public int compareTo(Measurement measurement) {
        return Double.compare(this.distanceCentimeters, measurement.getDistanceCentimeters());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Measurement that = (Measurement) o;
        return id == that.id &&
                Double.compare(that.distanceCentimeters, distanceCentimeters) == 0 &&
                Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, distanceCentimeters, time);
    }

    @NonNull
    @Override
    public String toString() {
        return "" + distanceCentimeters;
    }
}
