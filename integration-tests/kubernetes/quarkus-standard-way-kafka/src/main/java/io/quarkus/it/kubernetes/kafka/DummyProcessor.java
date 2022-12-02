package io.quarkus.it.kubernetes.kafka;

import java.util.Random;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.common.annotation.Blocking;

public class DummyProcessor {

    private final Random random = new Random();

    @Incoming("requests")
    @Outgoing("quotes")
    @Blocking
    public int process(String quoteRequest) throws InterruptedException {
        // simulate some hard working task
        Thread.sleep(200);
        return random.nextInt(100);
    }
}
