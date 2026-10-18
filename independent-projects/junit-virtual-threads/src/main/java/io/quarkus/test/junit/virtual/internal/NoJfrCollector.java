package io.quarkus.test.junit.virtual.internal;

import java.util.List;

import io.smallrye.common.annotation.SuppressForbidden;
import jdk.jfr.consumer.RecordedEvent;

public class NoJfrCollector implements Collector {

    @Override
    public boolean isRecording() {
        return false;
    }

    @Override
    public void init() {
    }

    @Override
    public void start() {
    }

    @Override
    public List<RecordedEvent> stop() {
        return List.of();
    }

    @SuppressForbidden(reason = "java.util.logging is authorized here")
    @Override
    public void shutdown() {
    }

    @Override
    public void accept(RecordedEvent re) {
    }

    @Override
    public List<RecordedEvent> getEvents() {
        return List.of();
    }
}
