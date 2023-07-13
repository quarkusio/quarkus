package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.HttpCommonTags;
import io.quarkus.micrometer.runtime.binder.RequestMetricInfo;
import io.vertx.core.http.WebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;

class VertxHttpClientMetrics extends VertxTcpClientMetrics
        implements HttpClientMetrics<VertxHttpClientMetrics.RequestTracker, String, LongTaskTimer.Sample, EventTiming> {

    private final LongAdder queue = new LongAdder();

    private final LongAdder pending = new LongAdder();

    private final Timer queueDelay;
    private final Map<String, LongAdder> webSockets = new ConcurrentHashMap<>();
    private final HttpBinderConfiguration config;

    VertxHttpClientMetrics(MeterRegistry registry, String prefix, Tags tags, HttpBinderConfiguration httpBinderConfiguration) {
        super(registry, prefix, tags);
        this.config = httpBinderConfiguration;
        queueDelay = Timer.builder("http.client.queue.delay")
                .description("Time spent in the waiting queue before being processed")
                .tags(tags)
                .register(registry);

        Gauge.builder("http.client.queue.size", new Supplier<Number>() {
            @Override
            public Number get() {
                return queue.doubleValue();
            }
        })
                .description("Number of pending elements in the waiting queue")
                .tags(tags)
                .strongReference(true)
                .register(registry);

        Gauge.builder("http.client.pending", new Supplier<Number>() {
            @Override
            public Number get() {
                return pending.longValue();
            }
        }).description("Number of requests waiting for a response");
    }

    @Override
    public ClientMetrics<RequestTracker, EventTiming, HttpRequest, HttpResponse> createEndpointMetrics(
            SocketAddress remoteAddress, int maxPoolSize) {
        String remote = NetworkMetrics.toString(remoteAddress);
        return new ClientMetrics<RequestTracker, EventTiming, HttpRequest, HttpResponse>() {
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
            public RequestTracker requestBegin(String uri, HttpRequest request) {
                RequestTracker handler = new RequestTracker(tags, remote, request.uri(), request.method().name());
                String path = handler.getNormalizedUriPath(
                        config.getServerMatchPatterns(),
                        config.getServerIgnorePatterns());
                if (path != null) {
                    pending.increment();
                    handler.timer = new EventTiming(null);
                    return handler;
                }
                return null;
            }

            @Override
            public void requestEnd(RequestTracker tracker, long bytesWritten) {
                if (tracker == null) {
                    return;
                }
                if (tracker.requestEnded()) {
                    pending.decrement();
                }
            }

            @Override
            public void requestReset(RequestTracker tracker) {
                if (tracker == null) {
                    return;
                }
                pending.decrement();
                tracker.requestReset();
            }

            @Override
            public void responseBegin(RequestTracker tracker, HttpResponse response) {
                if (tracker == null) {
                    return;
                }
                tracker.response = response;
            }

            @Override
            public void responseEnd(RequestTracker tracker, long bytesRead) {
                if (tracker == null) {
                    return;
                }
                if (tracker.responseEnded()) {
                    pending.decrement();
                }
                long duration = tracker.timer.end();
                Tags list = tracker.tags
                        .and(HttpCommonTags.status(tracker.response.statusCode()))
                        .and(HttpCommonTags.outcome(tracker.response.statusCode()));
                Timer.builder(config.getHttpClientRequestsName())
                        .description("Response times")
                        .tags(list)
                        .register(registry).record(duration, TimeUnit.NANOSECONDS);
            }
        };
    }

    @Override
    public String connected(WebSocket webSocket) {
        String remote = webSocket.remoteAddress().toString();
        webSockets.computeIfAbsent(remote, new Function<>() {
            @Override
            public LongAdder apply(String s) {
                LongAdder count = new LongAdder();
                Gauge.builder(config.getHttpClientWebSocketConnectionsName(), count::longValue)
                        .description("The number of active web socket connections")
                        .tags(tags.and("address", remote))
                        .register(registry);
                return count;
            }
        }).increment();
        return remote;
    }

    @Override
    public void disconnected(String remote) {
        var adder = webSockets.get(remote);
        if (adder != null) {
            adder.decrement();
            if (adder.longValue() == 0) {
                webSockets.remove(remote);
            }
        }
    }

    public static class RequestTracker extends RequestMetricInfo {
        private final Tags tags;
        private final String path;
        private EventTiming timer;
        HttpResponse response;
        private boolean responseEnded;
        private boolean requestEnded;
        private boolean reset;

        RequestTracker(Tags origin, String address, String path, String method) {
            this.path = path;
            this.tags = origin.and(
                    Tag.of("address", address),
                    HttpCommonTags.method(method),
                    HttpCommonTags.uri(path, -1));
        }

        void requestReset() {
            reset = true;
        }

        boolean requestEnded() {
            requestEnded = true;
            return !reset && responseEnded;
        }

        boolean responseEnded() {
            responseEnded = true;
            return !reset && requestEnded;
        }

        public String getNormalizedUriPath(Map<Pattern, String> serverMatchPatterns, List<Pattern> serverIgnorePatterns) {
            return super.getNormalizedUriPath(serverMatchPatterns, serverIgnorePatterns, path);
        }
    }
}
