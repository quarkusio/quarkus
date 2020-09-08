package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.http.Outcome;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.spi.metrics.HttpServerMetrics;

/**
 * HttpServerMetrics<R, W, S>
 * <ul>
 * <li>R for Request metric -- MetricsContext</li>
 * <li>W for Websocket metric -- LongTaskTimer sample</li>
 * <li>S for Socket metric -- MetricsContext</li>
 * </ul>
 */
public class VertxHttpServerMetrics extends VertxTcpMetrics
        implements HttpServerMetrics<MetricsContext, LongTaskTimer.Sample, MetricsContext> {
    static final Logger log = Logger.getLogger(VertxHttpServerMetrics.class);

    final List<Pattern> ignorePatterns;
    final List<Pattern> matchPatterns;

    final String nameWebsocketConnections;
    final String nameHttpServerPush;
    final String nameHttpServerRequests;

    VertxHttpServerMetrics(MeterRegistry registry, VertxConfig config) {
        super(registry, "http.server");
        nameWebsocketConnections = "http.server.websocket.connections";
        nameHttpServerPush = "http.server.push";
        nameHttpServerRequests = "http.server.requests";

        if (config.ignorePatterns.isPresent()) {
            List<String> stringPatterns = config.ignorePatterns.get();
            ignorePatterns = new ArrayList<>(stringPatterns.size());
            for (String s : stringPatterns) {
                ignorePatterns.add(Pattern.compile(s));
            }
        } else {
            ignorePatterns = Collections.emptyList();
        }

        if (config.matchPatterns.isPresent()) {
            List<String> stringPatterns = config.matchPatterns.get();
            matchPatterns = new ArrayList<>(stringPatterns.size());
            for (String s : stringPatterns) {
                matchPatterns.add(Pattern.compile(s));
            }
        } else {
            matchPatterns = Collections.emptyList();
        }
    }

    /**
     * Called when an http server response is pushed.
     *
     * @param socketMetric the socket metric
     * @param method the pushed response method
     * @param uri the pushed response uri
     * @param response the http server response
     * @return the request metric
     */
    @Override
    public MetricsContext responsePushed(MetricsContext socketMetric, HttpMethod method, String uri,
            HttpServerResponse response) {
        String path = VertxMetricsTags.parseUriPath(matchPatterns, ignorePatterns, uri);
        if (path != null) {
            registry.counter(nameHttpServerPush,
                    Tags.of(VertxMetricsTags.uri(path, response.getStatusCode()), VertxMetricsTags.method(method),
                            VertxMetricsTags.outcome(response), VertxMetricsTags.status(response.getStatusCode())))
                    .increment();
        }
        return socketMetric;
    }

    /**
     * Called when an http server request begins. Vert.x will invoke
     * {@link #responseEnd} when the response has ended or {@link #requestReset} if
     * the request/response has failed before.
     *
     * @param socketMetric the socket metric
     * @param request the http server reuqest
     * @return the request metric
     */
    @Override
    public MetricsContext requestBegin(MetricsContext socketMetric, HttpServerRequest request) {
        String path = VertxMetricsTags.parseUriPath(matchPatterns, ignorePatterns, request.uri());
        if (path != null) {
            // Pre-add the request method tag to the sample
            socketMetric.put(MetricsContext.HTTP_REQUEST_SAMPLE,
                    Timer.start(registry).tags(Tags.of(VertxMetricsTags.method(request.method()))));

            // remember the path to monitor for use later (maybe a 404 or redirect..)
            socketMetric.put(MetricsContext.HTTP_REQUEST_PATH, path);
        }
        return socketMetric;
    }

    /**
     * Called when the http server request couldn't complete successfully, for
     * instance the connection was closed before the response was sent.
     *
     * @param requestMetric the request metric
     */
    @Override
    public void requestReset(MetricsContext requestMetric) {
        Timer.Sample sample = requestMetric.getValue(MetricsContext.HTTP_REQUEST_SAMPLE);
        if (sample != null) {
            String requestPath = getServerRequestPath(requestMetric);
            sample.stop(registry,
                    Timer.builder(nameHttpServerRequests)
                            .tags(Tags.of(VertxMetricsTags.uri(requestPath, 0),
                                    Outcome.CLIENT_ERROR.asTag(), VertxMetricsTags.STATUS_RESET)));
        }
    }

    /**
     * Called when an http server response has ended.
     *
     * @param requestMetric the request metric
     * @param response the http server request
     */
    @Override
    public void responseEnd(MetricsContext requestMetric, HttpServerResponse response) {
        Timer.Sample sample = requestMetric.getValue(MetricsContext.HTTP_REQUEST_SAMPLE);
        if (sample != null) {
            String requestPath = getServerRequestPath(requestMetric);
            sample.stop(registry, Timer.builder(nameHttpServerRequests)
                    .tags(Tags.of(
                            VertxMetricsTags.uri(requestPath, response.getStatusCode()),
                            VertxMetricsTags.outcome(response),
                            VertxMetricsTags.status(response.getStatusCode()))));
        }
    }

    /**
     * Called when a server web socket connects.
     *
     * @param socketMetric the socket metric
     * @param requestMetric the request metric
     * @param serverWebSocket the server web socket
     * @return the server web socket metric
     */
    @Override
    public LongTaskTimer.Sample connected(MetricsContext socketMetric, MetricsContext requestMetric,
            ServerWebSocket serverWebSocket) {
        String path = getServerRequestPath(socketMetric);
        if (path != null) {
            return LongTaskTimer.builder(nameWebsocketConnections)
                    .tags(Tags.of(VertxMetricsTags.uri(path, 0)))
                    .register(registry)
                    .start();
        }
        return null;
    }

    /**
     * Called when the server web socket has disconnected.
     *
     * @param websocketMetric the server web socket metric
     */
    @Override
    public void disconnected(LongTaskTimer.Sample websocketMetric) {
        if (websocketMetric != null) {
            websocketMetric.stop();
        }
    }

    private String getServerRequestPath(MetricsContext source) {
        String path = source.getFromRoutingContext(MetricsContext.HTTP_REQUEST_PATH);
        if (path != null) {
            log.debugf("Using path from routing context %s", path);
            return path;
        }
        return source.getValue(MetricsContext.HTTP_REQUEST_PATH);
    }
}
