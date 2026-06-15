package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.http.Outcome;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.HttpCommonTags;
import io.quarkus.micrometer.runtime.export.exemplars.OpenTelemetryContextUnwrapper;
import io.quarkus.micrometer.runtime.meters.Gauges;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;

/**
 * HttpServerMetrics<R, W, S>
 * <ul>
 * <li>R for Request metric -- HttpRequestMetric</li>
 * <li>W for Websocket metric -- LongTaskTimer sample</li>
 * <li>S for Socket metric -- LongTaskTimer sample</li>
 * </ul>
 */
public class VertxHttpServerMetrics extends VertxTcpServerMetrics
        implements HttpServerMetrics<HttpRequestMetric, LongTaskTimer.Sample> {
    static final Logger log = Logger.getLogger(VertxHttpServerMetrics.class);

    HttpBinderConfiguration config;
    OpenTelemetryContextUnwrapper openTelemetryContextUnwrapper;

    final LongAdder activeRequests;

    final MeterProvider<Timer> requestsTimer;
    final MeterProvider<LongTaskTimer> websocketConnectionTimer;
    final MeterProvider<Counter> pushCounter;

    private final List<HttpServerMetricsTagsContributor> httpServerMetricsTagsContributors;

    VertxHttpServerMetrics(MeterRegistry registry,
            HttpBinderConfiguration config,
            OpenTelemetryContextUnwrapper openTelemetryContextUnwrapper, HttpServerConfig httpServerConfig,
            Gauges<LongAdder> gauges) {
        super(registry, "http.server", commonTags(httpServerConfig), gauges);
        this.config = config;
        this.openTelemetryContextUnwrapper = openTelemetryContextUnwrapper;

        Tags commonTags = commonTags(httpServerConfig);

        activeRequests = gauges.builder(config.getHttpServerActiveRequestsName(), LongAdder::doubleValue)
                .tag("url.scheme", httpServerConfig.getTcpConfig().isSsl() ? "https" : "http")
                .tags(commonTags)
                .register(registry);

        httpServerMetricsTagsContributors = resolveHttpServerMetricsTagsContributors();

        // not dev-mode changeable -----
        requestsTimer = Timer.builder(config.getHttpServerRequestsName())
                .description("HTTP server request processing time")
                .withRegistry(registry);

        websocketConnectionTimer = LongTaskTimer.builder(config.getHttpServerWebSocketConnectionsName())
                .description("Server web socket connection time")
                .withRegistry(registry);

        pushCounter = Counter.builder(config.getHttpServerPushName())
                .description("HTTP server response push counter")
                .withRegistry(registry);
        // not dev-mode changeable -----ˆ
    }

    private static Tags commonTags(HttpServerConfig httpServerConfig) {
        Tags result = Tags.empty();
        // we add a port tag (the one the application should actually bind to on the network host,
        // not the public one which we can't know easily) only if it's not random

        if (httpServerConfig.getTcpPort() > 0) {
            result = result.and("server.port", "" + httpServerConfig.getTcpPort());
        } else {
            result = result.and("server.port", "UNSET");
        }
        return result;
    }

    private List<HttpServerMetricsTagsContributor> resolveHttpServerMetricsTagsContributors() {
        final List<HttpServerMetricsTagsContributor> httpServerMetricsTagsContributors;
        ArcContainer arcContainer = Arc.container();
        if (arcContainer == null) {
            httpServerMetricsTagsContributors = Collections.emptyList();
        } else {
            var handles = arcContainer.listAll(HttpServerMetricsTagsContributor.class);
            if (handles.isEmpty()) {
                httpServerMetricsTagsContributors = Collections.emptyList();
            } else {
                httpServerMetricsTagsContributors = new ArrayList<>(handles.size());
                for (var handle : handles) {
                    httpServerMetricsTagsContributors.add(handle.get());
                }
            }
        }
        return httpServerMetricsTagsContributors;
    }

    /**
     * Called when an HTTP server response is pushed.
     *
     * @param SocketAddress the client remote address
     * @param method the pushed response method
     * @param uri the pushed response uri
     * @param response the http server response
     * @return a RequestMetricContext
     */
    @Override
    public HttpRequestMetric responsePushed(SocketAddress remoteAddress, HttpMethod method, String uri,
            HttpResponse response) {
        HttpRequestMetric requestMetric = new HttpRequestMetric(uri, activeRequests);
        String path = requestMetric.getNormalizedUriPath(
                config.getServerMatchPatterns(),
                config.getServerIgnorePatterns());
        if (path != null) {
            pushCounter
                    .withTags(Tags.of(
                            HttpCommonTags.uri(path, requestMetric.getInitialPath(), response.statusCode(),
                                    config.isServerSuppress4xxErrors()),
                            VertxMetricsTags.method(method),
                            VertxMetricsTags.outcome(response),
                            HttpCommonTags.status(response.statusCode())))
                    .increment();
        }
        log.debugf("responsePushed %s", requestMetric);
        return requestMetric;
    }

    @Override
    public void requestRouted(HttpRequestMetric requestMetric, String route) {
        log.debugf("requestRouted %s %s", route, requestMetric);
        requestMetric.appendCurrentRoutePath(route);
        if (route != null && requestMetric.request() != null) {
            var c = requestMetric.request().context();
            VertxContext.localContextData(c).put("VertxRoute", route);
        }
    }

    @Override
    public void responseBegin(HttpRequestMetric requestMetric, HttpResponse response) {
        if (requestMetric.request() == null) {
            return;
        }
        io.vertx.core.Context current = io.vertx.core.Vertx.currentContext();
        if (current != null && current != requestMetric.request().context()
                && VertxContext.isDuplicatedContext(current)) {
            requestMetric.setExecutionContext(current);
        }
    }

    /**
     * Called when an HTTP server request begins. Vert.x will invoke
     * {@link #responseEnd} when the response has ended or {@link #requestReset} if
     * the request/response has failed before.
     *
     * @param remoteAddress the client remote address
     * @param request the http server request
     * @return a RequestMetricContext
     */
    @Override
    public HttpRequestMetric requestBegin(SocketAddress remoteAddress, HttpRequest request) {
        HttpRequestMetric requestMetric = new HttpRequestMetric(request, activeRequests);
        requestMetric.setSample(Timer.start(registry));
        requestMetric.requestStarted();
        return requestMetric;
    }

    /**
     * Called when the http server request couldn't complete successfully, for
     * instance the connection was closed before the response was sent.
     *
     * @param requestMetric a RequestMetricContext or null
     */
    @Override
    public void requestReset(HttpRequestMetric requestMetric) {
        if (requestMetric == null) {
            return;
        }
        log.debugf("requestReset %s", requestMetric);

        String path = requestMetric.getNormalizedUriPath(
                config.getServerMatchPatterns(),
                config.getServerIgnorePatterns());
        if (path != null) {
            Timer.Sample sample = requestMetric.getSample();

            io.vertx.core.Context ctx = requestMetric.request() != null ? requestMetric.request().context() : null;
            openTelemetryContextUnwrapper.executeInContext(
                    sample::stop,
                    requestsTimer.withTags(Tags.of(
                            VertxMetricsTags.method(requestMetric.httpRequest().method()),
                            HttpCommonTags.uri(path, requestMetric.getInitialPath(), 0, false),
                            Outcome.CLIENT_ERROR.asTag(),
                            HttpCommonTags.STATUS_RESET)),
                    ctx);
        }
        requestMetric.requestEnded();
    }

    /**
     * Called when an HTTP server response has ended.
     *
     * @param requestMetric a RequestMetricContext or null
     * @param response the http server response
     * @param bytesWritten bytes written
     */
    @Override
    public void responseEnd(HttpRequestMetric requestMetric, HttpResponse response, long bytesWritten) {
        log.debugf("responseEnd %s, %s", response, requestMetric);

        String path = requestMetric.getNormalizedUriPath(
                config.getServerMatchPatterns(),
                config.getServerIgnorePatterns());
        if (path != null) {
            Timer.Sample sample = requestMetric.getSample();
            Tags allTags = Tags.of(
                    VertxMetricsTags.method(requestMetric.httpRequest().method()),
                    HttpCommonTags.uri(path, requestMetric.getInitialPath(), response.statusCode(),
                            config.isServerSuppress4xxErrors()),
                    VertxMetricsTags.outcome(response),
                    HttpCommonTags.status(response.statusCode()));
            if (!httpServerMetricsTagsContributors.isEmpty()) {
                HttpServerMetricsTagsContributor.Context context = new DefaultContext(requestMetric.httpRequest(), response,
                        requestMetric.request(), requestMetric.getExecutionContext());
                for (int i = 0; i < httpServerMetricsTagsContributors.size(); i++) {
                    try {
                        Tags additionalTags = httpServerMetricsTagsContributors.get(i).contribute(context);
                        allTags = allTags.and(additionalTags);
                    } catch (Exception e) {
                        log.debug("Unable to obtain additional tags", e);
                    }
                }
            }

            io.vertx.core.Context ctx = requestMetric.request() != null ? requestMetric.request().context() : null;
            openTelemetryContextUnwrapper.executeInContext(
                    sample::stop,
                    requestsTimer.withTags(allTags),
                    ctx);
        }
        requestMetric.requestEnded();
    }

    /**
     * Called when a server web socket connects.
     *
     * @param request the observable request
     * @return a LongTaskTimer.Sample or null
     */
    @Override
    public LongTaskTimer.Sample connected(HttpRequest request) {
        HttpRequestMetric requestMetric = new HttpRequestMetric(request, activeRequests);
        String path = requestMetric.getNormalizedUriPath(
                config.getServerMatchPatterns(),
                config.getServerIgnorePatterns());
        if (path != null) {
            return websocketConnectionTimer
                    .withTags(Tags.of(HttpCommonTags.uri(path, requestMetric.getInitialPath(), 0, false)))
                    .start();
        }
        return null;
    }

    /**
     * Called when the server web socket has disconnected.
     *
     * @param websocketMetric a LongTaskTimer.Sample or null
     */
    @Override
    public void disconnected(LongTaskTimer.Sample websocketMetric) {
        log.debugf("websocket disconnected %s", websocketMetric);
        if (websocketMetric != null) {
            websocketMetric.stop();
        }
    }

    private record DefaultContext(HttpRequest httpRequest,
            HttpResponse response,
            HttpServerRequestInternal requestInternal,
            io.vertx.core.Context executionContext) implements HttpServerMetricsTagsContributor.Context {
        @Override
        public HttpServerRequest request() {
            return httpRequest instanceof HttpServerRequest ? (HttpServerRequest) httpRequest : null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T requestContextLocalData(Object key) {
            if (executionContext != null && VertxContext.isDuplicatedContext(executionContext)) {
                T value = (T) VertxContext.localContextData(executionContext).get(key);
                if (value != null) {
                    return value;
                }
            }
            if (requestInternal != null) {
                var c = requestInternal.context();
                if (VertxContext.isDuplicatedContext(c)) {
                    return (T) VertxContext.localContextData(c).get(key);
                }
            }
            return null;
        }
    }
}
