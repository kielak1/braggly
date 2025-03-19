// src/main/java/com/example/backend/model/XrdData.java
package com.example.backend.model;

import java.util.List;

public class XrdData {
    private List<Double> angles;  // 2θ
    private List<Integer> intensities;  // Intensywności
    private List<Peak> peaks;  // Zidentyfikowane piki

    public XrdData(List<Double> angles, List<Integer> intensities, List<Peak> peaks) {
        this.angles = angles;
        this.intensities = intensities;
        this.peaks = peaks;
    }

    // Gettery i settery
    public List<Double> getAngles() { return angles; }
    public List<Integer> getIntensities() { return intensities; }
    public List<Peak> getPeaks() { return peaks; }
}

