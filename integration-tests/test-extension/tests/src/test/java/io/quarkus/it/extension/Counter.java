package io.quarkus.it.extension;

import java.util.concurrent.atomic.AtomicInteger;

public class Counter {

    public static final AtomicInteger startCounter = new AtomicInteger(0);
    public static final AtomicInteger endCounter = new AtomicInteger(0);
}
