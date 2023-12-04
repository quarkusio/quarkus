package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.http.Outcome;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.HttpCommonTags;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
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
        implements HttpServerMetrics<HttpRequestMetric, LongTaskTimer.Sample, LongTaskTimer.Sample> {
    static final Logger log = Logger.getLogger(VertxHttpServerMetrics.class);

    HttpBinderConfiguration config;

    final String nameWebsocketConnections;
    final String nameHttpServerPush;
    final String nameHttpServerRequests;
    final LongAdder activeRequests;

    private final List<HttpServerMetricsTagsContributor> httpServerMetricsTagsContributors;

    VertxHttpServerMetrics(MeterRegistry registry, HttpBinderConfiguration config) {
        super(registry, "http.server", null);
        this.config = config;

        // not dev-mode changeable
        nameWebsocketConnections = config.getHttpServerWebSocketConnectionsName();
        nameHttpServerPush = config.getHttpServerPushName();
        nameHttpServerRequests = config.getHttpServerRequestsName();

        activeRequests = new LongAdder();
        Gauge.builder(config.getHttpServerActiveRequestsName(), activeRequests, LongAdder::doubleValue)
                .register(registry);

        httpServerMetricsTagsContributors = resolveHttpServerMetricsTagsContributors();
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
     * @param socketMetric a Map for socket metric context or null
     * @param method the pushed response method
     * @param uri the pushed response uri
     * @param response the http server response
     * @return a RequestMetricContext
     */
    @Override
    public HttpRequestMetric responsePushed(LongTaskTimer.Sample socketMetric, HttpMethod method, String uri,
            HttpResponse response) {
        HttpRequestMetric requestMetric = new HttpRequestMetric(uri, activeRequests);
        String path = requestMetric.getNormalizedUriPath(
                config.getServerMatchPatterns(),
                config.getServerIgnorePatterns());
        if (path != null) {
            registry.counter(nameHttpServerPush, Tags.of(
                    HttpCommonTags.uri(path, response.statusCode()),
                    VertxMetricsTags.method(method),
                    VertxMetricsTags.outcome(response),
                    HttpCommonTags.status(response.statusCode())))
                    .increment();
        }
        log.debugf("responsePushed %s, %s", socketMetric, requestMetric);
        return requestMetric;
    }

    @Override
    public void requestRouted(HttpRequestMetric requestMetric, String route) {
        log.debugf("requestRouted %s %s", route, requestMetric);
        requestMetric.appendCurrentRoutePath(route);
        if (route != null) {
            requestMetric.request().context().putLocal("VertxRoute", route);
        }
    }

    /**
     * Called when an HTTP server request begins. Vert.x will invoke
     * {@link #responseEnd} when the response has ended or {@link #requestReset} if
     * the request/response has failed before.
     *
     * @param sample the sample
     * @param request the http server request
     * @return a RequestMetricContext
     */
    @Override
    public HttpRequestMetric requestBegin(LongTaskTimer.Sample sample, HttpRequest request) {
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
        log.debugf("requestReset %s", requestMetric);

        String path = requestMetric.getNormalizedUriPath(
                config.getServerMatchPatterns(),
                config.getServerIgnorePatterns());
        if (path != null) {
            Timer.Sample sample = requestMetric.getSample();
            Timer.Builder builder = Timer.builder(nameHttpServerRequests)
                    .tags(Tags.of(
                            VertxMetricsTags.method(requestMetric.request().method()),
                            HttpCommonTags.uri(path, 0),
                            Outcome.CLIENT_ERROR.asTag(),
                            HttpCommonTags.STATUS_RESET));

            sample.stop(builder.register(registry));
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
                    VertxMetricsTags.method(requestMetric.request().method()),
                    HttpCommonTags.uri(path, response.statusCode()),
                    VertxMetricsTags.outcome(response),
                    HttpCommonTags.status(response.statusCode()));
            if (!httpServerMetricsTagsContributors.isEmpty()) {
                HttpServerMetricsTagsContributor.Context context = new DefaultContext(requestMetric.request());
                for (int i = 0; i < httpServerMetricsTagsContributors.size(); i++) {
                    try {
                        Tags additionalTags = httpServerMetricsTagsContributors.get(i).contribute(context);
                        allTags = allTags.and(additionalTags);
                    } catch (Exception e) {
                        log.debug("Unable to obtain additional tags", e);
                    }
                }
            }
            Timer.Builder builder = Timer.builder(nameHttpServerRequests).tags(allTags);

            sample.stop(builder.register(registry));
        }
        requestMetric.requestEnded();
    }

    /**
     * Called when a server web socket connects.
     *
     * @param socketMetric a Map for socket metric context or null
     * @param requestMetric a RequestMetricContext or null
     * @param serverWebSocket the server web socket
     * @return a LongTaskTimer.Sample or null
     */
    @Override
    public LongTaskTimer.Sample connected(LongTaskTimer.Sample sample, HttpRequestMetric requestMetric,
            ServerWebSocket serverWebSocket) {
        String path = requestMetric.getNormalizedUriPath(
                config.getServerMatchPatterns(),
                config.getServerIgnorePatterns());
        if (path != null) {
            return LongTaskTimer.builder(nameWebsocketConnections)
                    .tags(Tags.of(HttpCommonTags.uri(path, 0)))
                    .register(registry)
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

    private static class DefaultContext implements HttpServerMetricsTagsContributor.Context {
        private final HttpServerRequest request;

        private DefaultContext(HttpServerRequest request) {
            this.request = request;
        }

        @Override
        public HttpServerRequest request() {
            return request;
        }
    }
}
