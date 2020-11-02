package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.http.Outcome;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.spi.metrics.HttpServerMetrics;

/**
 * HttpServerMetrics<R, W, S>
 * <ul>
 * <li>R for Request metric -- RequestMetricContext</li>
 * <li>W for Websocket metric -- LongTaskTimer sample</li>
 * <li>S for Socket metric -- Map<String, Object></li>
 * </ul>
 */
public class VertxHttpServerMetrics extends VertxTcpMetrics
        implements HttpServerMetrics<RequestMetric, LongTaskTimer.Sample, Map<String, Object>> {
    static final Logger log = Logger.getLogger(VertxHttpServerMetrics.class);

    final List<Pattern> ignorePatterns;
    final Map<Pattern, String> matchPatterns;

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
            matchPatterns = new HashMap<>(stringPatterns.size());
            for (String s : stringPatterns) {
                int pos = s.indexOf("=");
                if (pos > 0 && s.length() > 2) {
                    String pattern = s.substring(0, pos);
                    String replacement = s.substring(pos + 1);
                    try {
                        matchPatterns.put(Pattern.compile(pattern), replacement);
                    } catch (PatternSyntaxException pse) {
                        log.errorf("Invalid pattern in replacement string (%s=%s): %s", pattern, replacement, pse);
                    }
                } else {
                    log.errorf("Invalid pattern in replacement string (%s). Should be pattern=replacement", s);
                }
            }
        } else {
            matchPatterns = Collections.emptyMap();
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
    public RequestMetric responsePushed(Map<String, Object> socketMetric, HttpMethod method, String uri,
            HttpServerResponse response) {
        RequestMetric requestMetric = new RequestMetric();
        VertxMetricsTags.parseUriPath(requestMetric, matchPatterns, ignorePatterns, uri);
        if (requestMetric.measure) {
            registry.counter(nameHttpServerPush, Tags.of(
                    VertxMetricsTags.uri(requestMetric.path, response.getStatusCode()),
                    VertxMetricsTags.method(method),
                    VertxMetricsTags.outcome(response),
                    VertxMetricsTags.status(response.getStatusCode())))
                    .increment();
        }
        log.debugf("responsePushed %s: %s, %s", uri, socketMetric, requestMetric);
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
    public RequestMetric requestBegin(Map<String, Object> socketMetric, HttpServerRequest request) {
        RequestMetric requestMetric = new RequestMetric();
        RequestMetric.setRequestMetric(Vertx.currentContext(), requestMetric);

        // evaluate and remember the path to monitor for use later (maybe a 404 or redirect..)
        VertxMetricsTags.parseUriPath(requestMetric, matchPatterns, ignorePatterns, request.path());
        if (requestMetric.measure) {
            // If we're measuring this request, create/remember the sample
            requestMetric.sample = Timer.start(registry);
            requestMetric.tags = Tags.of(VertxMetricsTags.method(request.method()));

            log.debugf("requestBegin %s: %s, %s", requestMetric.path, socketMetric, requestMetric);
        }

        return requestMetric;
    }

    /**
     * Called when the http server request couldn't complete successfully, for
     * instance the connection was closed before the response was sent.
     *
     * @param requestMetric a RequestMetricContext or null
     */
    @Override
    public void requestReset(RequestMetric requestMetric) {
        log.debugf("requestReset: %s", requestMetric);
        Timer.Sample sample = getRequestSample(requestMetric);
        if (sample != null) {
            String requestPath = getServerRequestPath(requestMetric);
            Timer.Builder builder = Timer.builder(nameHttpServerRequests)
                    .tags(requestMetric.tags)
                    .tags(Tags.of(
                            VertxMetricsTags.uri(requestPath, 0),
                            Outcome.CLIENT_ERROR.asTag(),
                            VertxMetricsTags.STATUS_RESET));
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
    public void responseEnd(RequestMetric requestMetric, HttpServerResponse response) {
        log.debugf("responseEnd: %s, %s", requestMetric, response);

        Timer.Sample sample = getRequestSample(requestMetric);
        if (sample != null) {
            String requestPath = getServerRequestPath(requestMetric);
            Timer.Builder builder = Timer.builder(nameHttpServerRequests)
                    .tags(requestMetric.tags)
                    .tags(Tags.of(
                            VertxMetricsTags.uri(requestPath, response.getStatusCode()),
                            VertxMetricsTags.outcome(response),
                            VertxMetricsTags.status(response.getStatusCode())));

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
    public LongTaskTimer.Sample connected(Map<String, Object> socketMetric, RequestMetric requestMetric,
            ServerWebSocket serverWebSocket) {
        log.debugf("websocket connected: %s, %s, %s", socketMetric, requestMetric, serverWebSocket);
        String path = getServerRequestPath(requestMetric);
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
     * @param websocketMetric a LongTaskTimer.Sample or null
     */
    @Override
    public void disconnected(LongTaskTimer.Sample websocketMetric) {
        log.debugf("websocket disconnected: %s", websocketMetric);
        if (websocketMetric != null) {
            websocketMetric.stop();
        }
    }

    private Timer.Sample getRequestSample(RequestMetric metricsContext) {
        if (metricsContext == null) {
            return null;
        }
        return metricsContext.sample;
    }

    private String getServerRequestPath(RequestMetric metricsContext) {
        if (metricsContext == null) {
            return null;
        }
        return metricsContext.getHttpRequestPath();
    }
}
