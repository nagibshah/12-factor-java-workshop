package com.twelvefactor.platedetected;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AppTest {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        App function = new App();
        NumberPlateTrigger payload = new NumberPlateTrigger("testbuket","/plateimgaes/image1.jpg","jpg",100L,5);
        Object result = function.handleRequest(payload, null);
        assertEquals(payload, result);
    }
}
