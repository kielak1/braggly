// src/main/java/com/example/backend/model/Peak.java
package com.example.backend.model;

public class Peak {
    private double angle;  // Pozycja piku (2θ)
    private int intensity;  // Intensywność piku
    private double dSpacing;  // Odległość międzypłaszczyznowa (Å)

    public Peak(double angle, int intensity, double dSpacing) {
        this.angle = angle;
        this.intensity = intensity;
        this.dSpacing = dSpacing;
    }

    // Gettery i settery
    public double getAngle() { return angle; }
    public int getIntensity() { return intensity; }
    public double getDSpacing() { return dSpacing; }
}