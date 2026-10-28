package io.quarkus.load.shedding.runtime;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

@Singleton
public class HttpLoadShedding {
    public static final int PRIORITY = -1_000_000_000;

    public void init(@Observes @Priority(PRIORITY) Router router, OverloadDetector detector,
            PriorityLoadShedding priority, ExemptPaths exemptPaths, Instance<LoadSheddingMetrics> metricsInstance,
            LoadSheddingRuntimeConfig config) {

        if (!config.enabled()) {
            return;
        }

        LoadSheddingMetrics metrics = metricsInstance.isResolvable() ? metricsInstance.get() : null;

        router.route().order(PRIORITY).handler(ctx -> {
            if (detector.isOverloaded() && !exemptPaths.contains(ctx) && priority.shedLoad(ctx, detector.overloadDepth())) {
                if (metrics != null) {
                    metrics.requestShed();
                }

                // The connection is deliberately left open. Closing it would force the client
                // through a new TCP handshake (and most likely also TLS handshake) before it
                // could send anything else, and that handshake completes below this handler,
                // so it is load that can never be shed.
                HttpServerResponse response = ctx.response();
                response.setStatusCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code());
                long retryAfter = retryAfterSeconds(detector.overloadDurationNanos(), config.maxRetryAfter());
                response.headers().add(HttpHeaderNames.RETRY_AFTER, Long.toString(retryAfter));
                response.end();
            } else {
                detector.requestBegin();
                long start = System.nanoTime();
                ctx.addEndHandler(new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> ignored) {
                        long end = System.nanoTime();
                        detector.requestEnd((end - start) / 1_000);
                    }
                });
                ctx.next();
            }
        });
    }

    // How long clients are told to stay away follows how long the overload has already
    // lasted, as determined by `OverloadDetector.overloadDurationNanos()`. A brief spike
    // therefore sends them back quickly and a sustained overload keeps them away for longer.
    //
    // The result is then jittered, because a fixed value would make every client rejected in
    // the same instant come back in the same instant, which may easily make the overload
    // situation worse. The base is capped at half of the maximum, so that the jitter neither
    // exceeds the maximum nor collapses once the overload lasts longer than the cap.
    static long retryAfterSeconds(long overloadDurationNanos, Duration maxRetryAfter) {
        // The base has a floor of one second and the jitter doubles it, so a maximum of one
        // second or less cannot bound the result on its own; the header can express nothing
        // shorter than a second anyway. For any larger maximum, `2 * base <= 2 * half <= max`
        // already holds and the final cap never binds
        long max = Math.max(1, maxRetryAfter.toSeconds());
        long half = Math.max(1, max / 2);
        long elapsed = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(overloadDurationNanos));
        long base = Math.min(half, elapsed);
        return Math.min(max, ThreadLocalRandom.current().nextLong(base, 2 * base + 1));
    }
}
