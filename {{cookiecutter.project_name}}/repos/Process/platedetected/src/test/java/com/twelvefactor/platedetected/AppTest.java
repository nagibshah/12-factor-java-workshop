package com.twelvefactor.platedetected;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AppTest {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        App function = new App();
        NumberPlateTrigger payload = new NumberPlateTrigger();
        payload.setBucket("testbucket");
        payload.setKey("/plateimgaes/image1.jpg");
        payload.setContentType("jpg");
        payload.setContentLength(100L);
        payload.setCharge(5);
        Object result = function.handleRequest(payload, null);
        assertEquals(payload, result);
    }
}
