package com.cutter72.ultrasonicsensor.sensor.solids;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

public class Measurement implements Comparable<Measurement> {
    public static final int INITIAL_ID = 0;
    public static int nextId = INITIAL_ID;
    private final int id;
    private final double distanceCentimeters;
    private Date date;

    public Measurement(double distanceCentimeters) {
        this.distanceCentimeters = distanceCentimeters;
        this.date = new Date();
        this.id = nextId;
        nextId++;
    }

    public static void resetId() {
        nextId = INITIAL_ID;
    }

    public int getId() {
        return id;
    }

    public double getDistanceCentimeters() {
        return distanceCentimeters;
    }

    public Date getDate() {
        return date;
    }

    public Measurement setDate(Date date) {
        this.date = date;
        return this;
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
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, distanceCentimeters, date);
    }

    @NonNull
    @Override
    public String toString() {
        return "" + distanceCentimeters;
    }
}
