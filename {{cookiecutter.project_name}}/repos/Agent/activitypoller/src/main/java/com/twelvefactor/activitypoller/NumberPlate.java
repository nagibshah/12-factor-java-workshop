package com.twelvefactor.activitypoller;

public class NumberPlate {
    public boolean detected;
    public String numberPlateString;
    public float confidence;
    public String numberPlateRegEx;

    public boolean isDetected() {
        return detected;
    }

    public void setDetected(boolean detected) {
        this.detected = detected;
    }

    public String getNumberPlateString() {
        return numberPlateString;
    }

    public void setNumberPlateString(String numberPlateString) {
        this.numberPlateString = numberPlateString;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public String getNumberPlateRegEx() {
        return numberPlateRegEx;
    }

    public void setNumberPlateRegEx(String numberPlateRegEx) {
        this.numberPlateRegEx = numberPlateRegEx;
    }
}
