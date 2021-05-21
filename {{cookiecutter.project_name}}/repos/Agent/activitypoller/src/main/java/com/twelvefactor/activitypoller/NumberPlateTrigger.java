package com.twelvefactor.activitypoller;

public class NumberPlateTrigger {
    public String bucket;
    public String key;
    public String contentType;
    public Long contentLength;
    public NumberPlate numberPlate;
    public int charge;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    public NumberPlate getNumberPlate() {
        return numberPlate;
    }

    public void setNumberPlate(NumberPlate numberPlate) {
        this.numberPlate = numberPlate;
    }

    public int getCharge() {
        return charge;
    }

    public void setCharge(int charge) {
        this.charge = charge;
    }
}
