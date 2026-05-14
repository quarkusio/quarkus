package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.atomic.LongAdder;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.micrometer.runtime.meters.Gauges;
import io.vertx.core.spi.metrics.ClientMetrics;

public class VertxClientMetrics
        implements ClientMetrics<EventTiming, EventTiming, Object, Object> {

    private final String type;
    private final Timer processing;
    private final LongAdder current;
    private final LongAdder queue;
    private final Counter resetCount;
    private final Counter completed;
    private final Timer queueDelay;

    VertxClientMetrics(MeterRegistry registry, String type, Tags tags, Gauges<LongAdder> gauges) {
        this.type = type;

        queueDelay = Timer.builder(name("queue.delay"))
                .description("Time spent in the waiting queue before being processed")
                .tags(tags)
                .register(registry);

        processing = Timer.builder(name("processing"))
                .description("Processing time, from request start to response end")
                .tags(tags)
                .register(registry);

        queue = gauges.builder(name("queue.size"), LongAdder::doubleValue)
                .description("Number of pending elements in the waiting queue")
                .tags(tags)
                .register(registry);

        current = gauges.builder(name("current"), LongAdder::doubleValue)
                .description("The number of requests currently handled by the client")
                .tags(tags)
                .register(registry);

        completed = Counter.builder(name("completed"))
                .description("Number of requests that have been handled by the client")
                .tags(tags)
                .register(registry);

        resetCount = Counter.builder(name("reset"))
                .description("Total number of resets")
                .tags(tags)
                .register(registry);
    }

    private String name(String suffix) {
        return type + "." + suffix;
    }

    @Override
    public EventTiming enqueueRequest() {
        queue.increment();
        return new EventTiming(queueDelay);
    }

    @Override
    public void dequeueRequest(EventTiming event) {
        queue.decrement();
        event.end();
    }

    @Override
    public EventTiming requestBegin(String uri, Object request) {
        current.increment();
        return new EventTiming(processing);
    }

    @Override
    public void requestEnd(EventTiming requestMetric) {
        // Ignoring request-alone metrics at the moment
    }

    @Override
    public void responseBegin(EventTiming requestMetric, Object response) {
        // Ignoring response-alone metrics at the moment
    }

    @Override
    public void requestReset(EventTiming event) {
        current.decrement();
        event.end();
        resetCount.increment();
    }

    @Override
    public void responseEnd(EventTiming event) {
        current.decrement();
        event.end();
        completed.increment();
    }

    @Override
    public void close() {
    }
}
