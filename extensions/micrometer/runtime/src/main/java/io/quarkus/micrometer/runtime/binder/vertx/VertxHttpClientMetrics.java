package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.micrometer.runtime.HttpClientMetricsTagsContributor;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.HttpCommonTags;
import io.quarkus.micrometer.runtime.binder.RequestMetricInfo;
import io.quarkus.micrometer.runtime.meters.Gauges;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;

class VertxHttpClientMetrics extends VertxTcpClientMetrics
        implements HttpClientMetrics<VertxHttpClientMetrics.RequestTracker, Void> {
    static final Logger log = Logger.getLogger(VertxHttpClientMetrics.class);

    private final LongAdder pending;

    private final HttpBinderConfiguration config;
    private final Meter.MeterProvider<Timer> responseTimes;
    private final boolean restClient;

    private final List<HttpClientMetricsTagsContributor> httpClientMetricsTagsContributors;

    VertxHttpClientMetrics(MeterRegistry registry, String prefix, Tags tags, HttpBinderConfiguration httpBinderConfiguration,
            boolean restClient, Gauges<LongAdder> gauges) {
        super(registry, prefix, tags, gauges);
        this.config = httpBinderConfiguration;
        this.restClient = restClient;

        pending = gauges.builder("http.client.pending", LongAdder::longValue)
                .description("Number of requests waiting for a response")
                .tags(tags)
                .register(registry);

        httpClientMetricsTagsContributors = resolveHttpClientMetricsTagsContributors();

        responseTimes = Timer.builder(config.getHttpClientRequestsName())
                .description("Response times")
                .withRegistry(registry);
    }

    private List<HttpClientMetricsTagsContributor> resolveHttpClientMetricsTagsContributors() {
        final List<HttpClientMetricsTagsContributor> httpClientMetricsTagsContributors;
        ArcContainer arcContainer = Arc.container();
        if (arcContainer == null) {
            httpClientMetricsTagsContributors = Collections.emptyList();
        } else {
            var handles = arcContainer.listAll(HttpClientMetricsTagsContributor.class);
            if (handles.isEmpty()) {
                httpClientMetricsTagsContributors = Collections.emptyList();
            } else {
                httpClientMetricsTagsContributors = new ArrayList<>(handles.size());
                for (var handle : handles) {
                    httpClientMetricsTagsContributors.add(handle.get());
                }
            }
        }
        return httpClientMetricsTagsContributors;
    }

    @Override
    public ClientMetrics<RequestTracker, HttpRequest, HttpResponse> createEndpointMetrics(
            SocketAddress remoteAddress, int maxPoolSize) {
        String remote = NetworkMetrics.toString(remoteAddress);
        return new ClientMetrics<RequestTracker, HttpRequest, HttpResponse>() {
            private final Deque<LongTaskTimer.Sample> connectionSamples = new ConcurrentLinkedDeque<>();

            @Override
            public void connected() {
                getConnCount().increment();
                connectionSamples.push(getConnDuration().start());
            }

            @Override
            public void disconnected() {
                getConnCount().decrement();
                LongTaskTimer.Sample sample = connectionSamples.poll();
                if (sample != null) {
                    sample.stop();
                }
            }

            @Override
            public RequestTracker init() {
                return new RequestTracker();
            }

            @Override
            public void requestBegin(RequestTracker tracker, String uri, HttpRequest request) {
                tracker.request = request;
                tracker.tags = tags.and(
                        Tag.of("address", remote),
                        HttpCommonTags.method(request.method().name()),
                        HttpCommonTags.uri(request.uri(), null, -1, false));
                String path = tracker.getNormalizedUriPath(
                        config.getServerMatchPatterns(),
                        config.getServerIgnorePatterns());
                if (path != null) {
                    pending.increment();
                    tracker.timer = new EventTiming(null);
                }
            }

            @Override
            public void requestEnd(RequestTracker tracker, long bytesWritten) {
                if (bytesWritten > 0) {
                    sent.record(bytesWritten);
                }
                if (!shouldTrack(tracker)) {
                    return;
                }
                if (tracker.requestEnded()) {
                    pending.decrement();
                }
            }

            @Override
            public void requestReset(RequestTracker tracker) {
                if (!shouldTrack(tracker)) {
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
                if (bytesRead > 0) {
                    received.record(bytesRead);
                }
                if (!shouldTrack(tracker)) {
                    return;
                }
                if (tracker.responseEnded()) {
                    pending.decrement();
                }
                if (restClient) {
                    return;
                }
                long duration = tracker.timer.end();
                Tags list = tracker.tags
                        .and(HttpCommonTags.status(tracker.response.statusCode()))
                        .and(HttpCommonTags.outcome(tracker.response.statusCode()));
                if (!httpClientMetricsTagsContributors.isEmpty()) {
                    HttpClientMetricsTagsContributor.Context context = new DefaultContext(tracker.request, tracker.response);
                    for (int i = 0; i < httpClientMetricsTagsContributors.size(); i++) {
                        try {
                            Tags additionalTags = httpClientMetricsTagsContributors.get(i).contribute(context);
                            list = list.and(additionalTags);
                        } catch (Exception e) {
                            log.debug("Unable to obtain additional tags", e);
                        }
                    }
                }

                responseTimes
                        .withTags(list)
                        .record(duration, TimeUnit.NANOSECONDS);
            }

            @Override
            public void close() {
            }
        };
    }

    private boolean shouldTrack(RequestTracker tracker) {
        if ((tracker == null) || tracker.timer == null) {
            return false;
        }
        if (tracker.isIgnored()) {
            return false;
        }
        return true;
    }

    public static class RequestTracker extends RequestMetricInfo implements IgnorableMetric {

        Tags tags;
        HttpRequest request;
        private EventTiming timer;
        HttpResponse response;
        private boolean responseEnded;
        private boolean requestEnded;
        private boolean reset;
        private volatile boolean ignored;

        public RequestTracker() {
        }

        RequestTracker(Tags origin, String address, HttpRequest request) {
            this.request = request;
            this.tags = origin.and(
                    Tag.of("address", address),
                    HttpCommonTags.method(request.method().name()),
                    HttpCommonTags.uri(request.uri(), null, -1, false));
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
            return super.getNormalizedUriPath(serverMatchPatterns, serverIgnorePatterns, request.uri());
        }

        @Override
        public void markAsIgnored() {
            ignored = true;
        }

        @Override
        public boolean isIgnored() {
            return ignored;
        }
    }

    private record DefaultContext(HttpRequest request,
            HttpResponse response) implements HttpClientMetricsTagsContributor.Context {
    }
}
