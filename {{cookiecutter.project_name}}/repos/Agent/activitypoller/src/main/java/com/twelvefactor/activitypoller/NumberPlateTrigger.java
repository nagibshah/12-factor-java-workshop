package com.twelvefactor.activitypoller;

public class NumberPlateTrigger {
    public String bucket;
    public String key;
    public String contentType;
    public Long contentLength;
    public NumberPlate numberPlate;
    public int charge;

    public NumberPlateTrigger(String bucket, String key, String contentType, Long contentLength, int charge) {
        this.bucket = bucket;
        this.key = key;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.charge = charge;
    }

    class NumberPlate {
        public boolean detected;
        public String numberPlateString;
        public float confidence;
        public String numberPlateRegEx;

        NumberPlate(String numberPlateRegEx, boolean detected) {
            this.detected = detected;
            this.numberPlateRegEx = numberPlateRegEx;
        }

    }
}


