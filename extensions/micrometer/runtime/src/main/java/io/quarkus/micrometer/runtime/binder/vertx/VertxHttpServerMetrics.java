package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.Map;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.http.Outcome;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.HttpCommonTags;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.ext.web.RoutingContext;

/**
 * HttpServerMetrics<R, W, S>
 * <ul>
 * <li>R for Request metric -- RequestMetricContext</li>
 * <li>W for Websocket metric -- LongTaskTimer sample</li>
 * <li>S for Socket metric -- Map<String, Object></li>
 * </ul>
 */
public class VertxHttpServerMetrics extends VertxTcpMetrics
        implements HttpServerMetrics<HttpRequestMetric, LongTaskTimer.Sample, Map<String, Object>> {
    static final Logger log = Logger.getLogger(VertxHttpServerMetrics.class);
    static final String METRICS_CONTEXT = "HTTP_REQUEST_METRICS_CONTEXT";

    HttpBinderConfiguration config;

    final String nameWebsocketConnections;
    final String nameHttpServerPush;
    final String nameHttpServerRequests;

    VertxHttpServerMetrics(MeterRegistry registry, HttpBinderConfiguration config) {
        super(registry, "http.server");
        this.config = config;

        // not dev-mode changeable
        nameWebsocketConnections = config.getHttpServerWebSocketConnectionsName();
        nameHttpServerPush = config.getHttpServerPushName();
        nameHttpServerRequests = config.getHttpServerRequestsName();
    }

    /**
     * Stash the RequestMetric in the Vertx Context
     *
     * @param context Vertx context to store RequestMetric in
     * @param requestMetric
     * @see VertxMeterFilter
     */
    public static void setRequestMetric(Context context, HttpRequestMetric requestMetric) {
        if (context != null) {
            context.put(METRICS_CONTEXT, requestMetric);
        }
    }

    /**
     * Move RequestMetric from the Vertx Context to the RoutingContext: YUCK
     * Note: dropped in Vert.x 4
     *
     * @param context
     * @param routingContext
     * @return the RequestMetricContext stored in the Vertx Context, or null
     * @see VertxMeterFilter
     * @see #setRequestMetric(Context, HttpRequestMetric)
     */
    public static void moveRequestMetric(Context context, RoutingContext routingContext) {
        if (context != null) {
            HttpRequestMetric requestMetric = context.get(METRICS_CONTEXT);
            if (requestMetric != null) {
                requestMetric.setRoutingContext(routingContext);
                context.remove(METRICS_CONTEXT);
                routingContext.put(METRICS_CONTEXT, requestMetric);
            }
        }
    }

    /**
     * Called when an http server response is pushed.
     *
     * @param socketMetric a Map for socket metric context or null
     * @param method the pushed response method
     * @param uri the pushed response uri
     * @param response the http server response
     * @return a RequestMetricContext
     */
    @Override
    public HttpRequestMetric responsePushed(Map<String, Object> socketMetric, HttpMethod method, String uri,
            HttpServerResponse response) {
        HttpRequestMetric requestMetric = new HttpRequestMetric(uri);
        String path = requestMetric.getNormalizedUriPath(
                config.getServerMatchPatterns(),
                config.getServerIgnorePatterns());
        if (path != null) {
            registry.counter(nameHttpServerPush, Tags.of(
                    HttpCommonTags.uri(path, response.getStatusCode()),
                    VertxMetricsTags.method(method),
                    VertxMetricsTags.outcome(response),
                    HttpCommonTags.status(response.getStatusCode())))
                    .increment();
        }
        log.debugf("responsePushed %s, %s", socketMetric, requestMetric);
        return requestMetric;
    }

    /**
     * Called when an http server request begins. Vert.x will invoke
     * {@link #responseEnd} when the response has ended or {@link #requestReset} if
     * the request/response has failed before.
     *
     * @param socketMetric a Map for socket metric context or null
     * @param request the http server request
     * @return a RequestMetricContext
     */
    @Override
    public HttpRequestMetric requestBegin(Map<String, Object> socketMetric, HttpServerRequest request) {
        HttpRequestMetric requestMetric = new HttpRequestMetric(request);
        requestMetric.setSample(Timer.start(registry));
        setRequestMetric(Vertx.currentContext(), requestMetric);

        log.debugf("requestBegin %s, %s", socketMetric, requestMetric);
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
    }

    /**
     * Called when an http server response has ended.
     *
     * @param requestMetric a RequestMetricContext or null
     * @param response the http server response
     */
    @Override
    public void responseEnd(HttpRequestMetric requestMetric, HttpServerResponse response) {
        log.debugf("responseEnd %s, %s", response, requestMetric);

        String path = requestMetric.getNormalizedUriPath(
                config.getServerMatchPatterns(),
                config.getServerIgnorePatterns());
        if (path != null) {
            Timer.Sample sample = requestMetric.getSample();
            Timer.Builder builder = Timer.builder(nameHttpServerRequests)
                    .tags(Tags.of(
                            VertxMetricsTags.method(requestMetric.request().method()),
                            HttpCommonTags.uri(path, response.getStatusCode()),
                            VertxMetricsTags.outcome(response),
                            HttpCommonTags.status(response.getStatusCode())));

            sample.stop(builder.register(registry));
        }
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
    public LongTaskTimer.Sample connected(Map<String, Object> socketMetric, HttpRequestMetric requestMetric,
            ServerWebSocket serverWebSocket) {
        log.debugf("websocket connected %s, %s, %s", socketMetric, serverWebSocket, requestMetric);

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
        log.debugf("websocket disconnected: %s", websocketMetric);
        if (websocketMetric != null) {
            websocketMetric.stop();
        }
    }
}
