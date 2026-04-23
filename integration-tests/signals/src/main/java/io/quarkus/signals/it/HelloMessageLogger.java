package io.quarkus.signals.it;

import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.logging.Log;
import io.quarkus.signals.Receives;

public class HelloMessageLogger {

    private final AtomicInteger counter = new AtomicInteger();

    void helloMessage(@Receives HelloMessage helloMessage) {
        counter.incrementAndGet();
        Log.infof("Hello message received: %s", helloMessage.text());
    }

    public int getCount() {
        return counter.get();
    }
}
