package com.example.ultrasonicsensor;

public class Impacts {
    private double ceiling;
    private double thickness;
    private double bottom;
    private double impacts;

    public Impacts(double ceiling, double bottom, double impacts) {
        this.ceiling = ceiling;
        this.bottom = bottom;
        this.impacts = impacts;
        this.thickness = Math.abs(ceiling - bottom);
    }

    public double getCeiling() {
        return ceiling;
    }

    public Impacts setCeiling(double ceiling) {
        this.ceiling = ceiling;
        return this;
    }

    public double getThickness() {
        return thickness;
    }

    public Impacts setThickness(double thickness) {
        this.thickness = thickness;
        return this;
    }

    public double getBottom() {
        return bottom;
    }

    public Impacts setBottom(double bottom) {
        this.bottom = bottom;
        return this;
    }

    public double getImpacts() {
        return impacts;
    }

    public Impacts setImpacts(double impacts) {
        this.impacts = impacts;
        return this;
    }
}
