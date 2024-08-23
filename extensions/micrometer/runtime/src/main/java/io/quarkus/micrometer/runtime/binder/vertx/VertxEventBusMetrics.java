package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.spi.metrics.EventBusMetrics;

public class VertxEventBusMetrics implements EventBusMetrics<VertxEventBusMetrics.Handler> {

    private final Tags tags;
    private final MeterRegistry registry;
    private final Handler ignored;

    private Map<String, Handler> handlers = new ConcurrentHashMap<>();

    private final Meter.MeterProvider<Counter> published;
    private final Meter.MeterProvider<Counter> sent;
    private final Meter.MeterProvider<DistributionSummary> written;
    private final Meter.MeterProvider<DistributionSummary> read;
    private final Meter.MeterProvider<Counter> replyFailures;

    VertxEventBusMetrics(MeterRegistry registry, Tags tags) {
        this.registry = registry;
        this.tags = tags;
        this.ignored = new Handler(null);

        published = Counter.builder("eventBus.published")
                .description("Number of messages published to the event bus")
                .withRegistry(registry);
        sent = Counter.builder("eventBus.sent")
                .description("Number of messages sent to the event bus")
                .withRegistry(registry);
        written = DistributionSummary.builder("eventBus.bytes.written")
                .description("Track the number of bytes written to the distributed event bus")
                .withRegistry(registry);
        read = DistributionSummary.builder("eventBus.bytes.read")
                .description("The number of bytes read from the distributed event bus")
                .withRegistry(registry);
        replyFailures = Counter.builder("eventBus.replyFailures")
                .description("Count the number of reply failure")
                .withRegistry(registry);
    }

    private static boolean isInternal(String address) {
        return address.startsWith("__vertx.");
    }

    @Override
    public Handler handlerRegistered(String address, String repliedAddress) {
        if (isInternal(address)) {
            // Ignore internal metrics
            return ignored;
        }
        return handlers.computeIfAbsent(address, a -> new Handler(address))
                .increment();

    }

    @Override
    public void handlerUnregistered(Handler handler) {
        if (isValid(handler)) {
            if (handlers.get(handler.address).decrement()) {
                handlers.remove(handler.address);
            }
        }
    }

    @Override
    public void scheduleMessage(Handler handler, boolean b) {
    }

    @Override
    public void messageDelivered(Handler handler, boolean local) {
        if (isValid(handler)) {
            handler.delivered();
        }
    }

    @Override
    public void discardMessage(Handler handler, boolean local, Message<?> msg) {
        if (isValid(handler)) {
            handler.discarded();
        }
    }

    @Override
    public void messageSent(String address, boolean publish, boolean local, boolean remote) {
        if (!isInternal(address)) {
            if (publish) {
                published.withTags(this.tags.and("address", address)).increment();
            } else {
                sent.withTags(this.tags.and("address", address)).increment();
            }
        }
    }

    @Override
    public void messageWritten(String address, int numberOfBytes) {
        if (!isInternal(address)) {
            written.withTags(this.tags.and("address", address)).record(numberOfBytes);
        }
    }

    @Override
    public void messageRead(String address, int numberOfBytes) {
        if (!isInternal(address)) {
            read.withTags(this.tags.and("address", address)).record(numberOfBytes);
        }
    }

    @Override
    public void replyFailure(String address, ReplyFailure failure) {
        if (!isInternal(address)) {
            replyFailures
                    .withTags(this.tags.and("address", address, "failure", failure.name()))
                    .increment();
        }
    }

    @Override
    public void close() {
    }

    private static boolean isValid(Handler handler) {
        return handler != null && handler.address != null;
    }

    class Handler {
        private final String address;
        private final LongAdder count;
        private final LongAdder delivered;
        private final LongAdder discarded;

        Handler(String address) {
            if (address == null) {
                this.address = null;
                this.count = null;
                this.delivered = null;
                this.discarded = null;
                return;
            }

            this.address = address;
            this.count = new LongAdder();
            this.delivered = new LongAdder();
            this.discarded = new LongAdder();
            Gauge.builder("eventBus.handlers", count::longValue)
                    .description("Number of handlers per address")
                    .tags(tags.and("address", address))
                    .register(registry);

            Gauge.builder("eventBus.delivered", delivered::longValue)
                    .description("Number of messages delivered")
                    .tags(tags.and("address", address))
                    .register(registry);

            Gauge.builder("eventBus.discarded", discarded::longValue)
                    .description("Number of messages discarded")
                    .tags(tags.and("address", address))
                    .register(registry);
        }

        public Handler increment() {
            count.increment();
            return this;
        }

        public boolean decrement() {
            count.decrement();
            return count.longValue() == 0;
        }

        public void delivered() {
            delivered.increment();
        }

        public void discarded() {
            discarded.increment();
        }
    }
}
