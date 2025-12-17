package io.quarkus.test.junit.virtual.internal;

import java.util.List;
import java.util.function.Consumer;

import jdk.jfr.consumer.RecordedEvent;

public interface Collector extends Consumer<RecordedEvent> {

    String CARRIER_PINNED_EVENT_NAME = "jdk.VirtualThreadPinned";

    static Collector create() {
        if (Boolean.getBoolean("jfr.unsupported.vm")) {
            return new NoJfrCollector();
        }

        return new JfrCollector();
    }

    void init();

    void start();

    List<RecordedEvent> stop();

    void shutdown();

    @Override
    void accept(RecordedEvent re);

    List<RecordedEvent> getEvents();

    boolean isRecording();
}
