package io.quarkus.test.junit5.virtual.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.smallrye.common.annotation.SuppressForbidden;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;

public class Collector implements Consumer<RecordedEvent> {
    public static final String CARRIER_PINNED_EVENT_NAME = "jdk.VirtualThreadPinned";
    private static final Logger LOGGER = Logger.getLogger(Collector.class.getName());

    private final List<Function<RecordedEvent, Boolean>> observers = new CopyOnWriteArrayList<>();

    private final List<RecordedEvent> events = new CopyOnWriteArrayList<>();

    private final RecordingStream recordingStream;

    volatile State state = State.INIT;

    public Collector() {
        recordingStream = new RecordingStream();
        recordingStream.enable(CARRIER_PINNED_EVENT_NAME).withStackTrace();
        recordingStream.enable(InternalEvents.SHUTDOWN_EVENT_NAME).withoutStackTrace();
        recordingStream.enable(InternalEvents.CAPTURING_STARTED_EVENT_NAME).withoutStackTrace();
        recordingStream.enable(InternalEvents.CAPTURING_STOPPED_EVENT_NAME).withoutStackTrace();
        recordingStream.enable(InternalEvents.INITIALIZATION_EVENT_NAME).withoutStackTrace();
        recordingStream.setOrdered(true);
        recordingStream.setMaxSize(100);
        recordingStream.onEvent(this);
    }

    @SuppressForbidden(reason = "java.util.logging is authorized here")
    public void init() {
        long begin = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(1);
        observers.add(re -> {
            if (re.getEventType().getName().equals(InternalEvents.INITIALIZATION_EVENT_NAME)) {
                latch.countDown();
                return true;
            }
            return false;
        });
        recordingStream.startAsync();
        new InternalEvents.InitializationEvent().commit();
        try {
            if (latch.await(10, TimeUnit.SECONDS)) {
                long end = System.nanoTime();
                state = State.STARTED;
                LOGGER.log(Level.FINE, "Event collection started in {0}s", (end - begin) / 1000000);
            } else {
                throw new IllegalStateException(
                        "Unable to start JFR collection, RecordingStartedEvent event not received after 10s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @SuppressForbidden(reason = "java.util.logging is authorized here")
    public void start() {
        CountDownLatch latch = new CountDownLatch(1);
        String id = UUID.randomUUID().toString();
        long begin = System.nanoTime();
        observers.add(re -> {
            if (re.getEventType().getName().equals(InternalEvents.CAPTURING_STARTED_EVENT_NAME)) {
                if (id.equals(re.getString("id"))) {
                    events.clear();
                    state = State.COLLECTING;
                    latch.countDown();
                    return true;
                }
            }
            return false;
        });

        new InternalEvents.CapturingStartedEvent(id).commit();

        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Unable to start JFR collection, START_EVENT event not received after 10s");
            }
            long end = System.nanoTime();
            LOGGER.log(Level.FINE, "Event capturing started in {0}s", (end - begin) / 1000000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @SuppressForbidden(reason = "java.util.logging is authorized here")
    public List<RecordedEvent> stop() {
        CountDownLatch latch = new CountDownLatch(1);
        String id = UUID.randomUUID().toString();
        var begin = System.nanoTime();
        observers.add(re -> {
            if (re.getEventType().getName().equals(InternalEvents.CAPTURING_STOPPED_EVENT_NAME)) {
                state = State.STARTED;
                latch.countDown();
                return true;
            }
            return false;
        });

        new InternalEvents.CapturingStoppedEvent(id).commit();

        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Unable to start JFR collection, STOP_EVENT event not received after 10s");
            }
            var end = System.nanoTime();
            LOGGER.log(Level.FINE, "Event collection stopped in {0}s", (end - begin) / 1000000);
            return new ArrayList<>(events);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @SuppressForbidden(reason = "java.util.logging is authorized here")
    public void shutdown() {
        CountDownLatch latch = new CountDownLatch(1);
        var begin = System.nanoTime();
        observers.add(re -> {
            if (re.getEventType().getName().equals(InternalEvents.SHUTDOWN_EVENT_NAME)) {
                latch.countDown();
                return true;
            }
            return false;
        });
        InternalEvents.ShutdownEvent event = new InternalEvents.ShutdownEvent();
        event.commit();
        try {
            if (latch.await(10, TimeUnit.SECONDS)) {
                state = State.INIT;
                var end = System.nanoTime();
                LOGGER.log(Level.FINE, "Event collector shutdown in {0}s", (end - begin) / 1000000);
                recordingStream.close();
            } else {
                throw new IllegalStateException(
                        "Unable to stop JFR collection, RecordingStoppedEvent event not received at 10s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accept(RecordedEvent re) {
        if (state == State.COLLECTING) {
            events.add(re);
        }
        List<Function<RecordedEvent, Boolean>> toBeRemoved = new ArrayList<>();
        observers.forEach(c -> {
            if (c.apply(re)) {
                toBeRemoved.add(c);
            }
        });
        observers.removeAll(toBeRemoved);
    }

    public List<RecordedEvent> getEvents() {
        return new ArrayList<>(events);
    }

    enum State {
        INIT,
        STARTED,
        COLLECTING
    }

}
