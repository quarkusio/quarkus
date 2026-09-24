package io.quarkus.it.arc.selfinvocation;

import java.time.Instant;

public class TestEnvelope implements Envelope<String> {

    private final String payload;
    private final Instant timestamp;
    private final String id;

    public TestEnvelope(String payload, Instant timestamp, String id) {
        this.payload = payload;
        this.timestamp = timestamp;
        this.id = id;
    }

    @Override
    public String payload() {
        return payload;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public String id() {
        return id;
    }
}
