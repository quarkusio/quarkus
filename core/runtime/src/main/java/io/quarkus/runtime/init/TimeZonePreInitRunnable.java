package io.quarkus.runtime.init;

import java.util.TimeZone;

public class TimeZonePreInitRunnable implements Runnable {

    @Override
    public void run() {
        TimeZone.getDefault().toZoneId();
    }
}