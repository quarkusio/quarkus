package io.quarkus.http.ratelimit.runtime.internal.telemetry;

import io.quarkus.http.ratelimit.runtime.internal.rules.Outcome;
import io.quarkus.http.ratelimit.runtime.internal.rules.RuleEvaluator;
import io.vertx.ext.web.RoutingContext;

/**
 * Internal hook for JDK Flight Recorder events. The JFR implementation is a bean only when the JFR extension is
 * present; otherwise {@link #NONE} is used and no JFR class of this extension is loaded. Called on the request's
 * context; implementations must not block.
 */
public interface RateLimitFlightRecorder {

    RateLimitFlightRecorder NONE = new RateLimitFlightRecorder() {
    };

    /**
     * @return a handle to pass to {@link #evaluated}, or {@code null} when the evaluation is not recorded
     */
    default Object evaluationStarted() {
        return null;
    }

    /**
     * @param evaluation the handle {@link #evaluationStarted()} returned
     */
    default void evaluated(Object evaluation, RoutingContext ctx, Outcome outcome) {
    }

    /**
     * The rule rejected the request with 429.
     */
    default void rejected(RoutingContext ctx, RuleEvaluator evaluator, long retryAfterSeconds) {
    }
}
