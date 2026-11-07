package io.quarkus.http.ratelimit.jfr.runtime.internal;

import jakarta.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.Unremovable;
import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.runtime.internal.rules.Outcome;
import io.quarkus.http.ratelimit.runtime.internal.rules.RuleEvaluator;
import io.quarkus.http.ratelimit.runtime.internal.telemetry.RateLimitFlightRecorder;
import io.quarkus.jfr.api.IdProducer;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.vertx.ext.web.RoutingContext;

/**
 * Commits the rate limit JFR events. A bean only when the JFR extension is present.
 * <p>
 * An evaluation event is begun only when it is enabled, and every event is populated only when it will be committed.
 */
@Singleton
@Unremovable
public class JfrRateLimitFlightRecorder implements RateLimitFlightRecorder {

    private static final String IDS_KEY = JfrRateLimitFlightRecorder.class.getName() + ".ids";

    private final IdProducer idProducer;

    public JfrRateLimitFlightRecorder(IdProducer idProducer) {
        this.idProducer = idProducer;
    }

    @Override
    public Object evaluationStarted() {
        RateLimitEvaluationEvent event = new RateLimitEvaluationEvent();
        if (!event.isEnabled()) {
            return null;
        }
        event.begin();
        return event;
    }

    /**
     * The decision is asynchronous: {@link #evaluationStarted()} and this method may run on different threads, which
     * JFR allows (begin and end only take timestamps; the event is not shared concurrently, as the stages of a
     * {@code Uni} happen one after another). The recorded thread is the one that commits.
     */
    @Override
    public void evaluated(Object evaluation, RoutingContext ctx, Outcome outcome) {
        if (!(evaluation instanceof RateLimitEvaluationEvent event)) {
            return;
        }
        event.end();
        if (event.shouldCommit()) {
            Ids ids = ids(ctx);
            event.traceId = ids.traceId;
            event.spanId = ids.spanId;
            RuleEvaluator evaluator = outcome.evaluator();
            event.rule = evaluator.rule();
            event.phase = evaluator.phase().configName();
            event.store = evaluator.storeName();
            event.outcome = outcome(outcome);
            event.commit();
        }
    }

    @Override
    public void rejected(RoutingContext ctx, RuleEvaluator evaluator, long retryAfterSeconds) {
        RateLimitRejectedEvent event = new RateLimitRejectedEvent();
        if (event.shouldCommit()) {
            Ids ids = ids(ctx);
            event.traceId = ids.traceId;
            event.spanId = ids.spanId;
            event.rule = evaluator.rule();
            event.phase = evaluator.phase().configName();
            event.httpMethod = ctx.request().method().name();
            // the path the rules were matched against
            event.path = HttpSecurityUtils.normalizePath(ctx.normalizedPath());
            event.retryAfter = retryAfterSeconds;
            event.commit();
        }
    }

    private static String outcome(Outcome outcome) {
        return switch (outcome.kind()) {
            case DECIDED -> switch (outcome.decision()) {
                case RateLimitDecision.Allowed allowed -> "ALLOWED";
                case RateLimitDecision.Rejected rejected -> "REJECTED";
                case RateLimitDecision.Unavailable unavailable -> "UNAVAILABLE";
            };
            case SKIPPED -> outcome.kind().name();
        };
    }

    /**
     * The IDs of the request, the same for every event of the request. The rate limit filters run before any CDI
     * request context exists; the request scoped {@link IdProducer} then needs one of its own, only for as long as
     * the IDs are read. Without OpenTelemetry, the trace ID of these events is therefore not that of the REST events of
     * the same request.
     */
    private Ids ids(RoutingContext ctx) {
        Ids ids = ctx.get(IDS_KEY);
        if (ids != null) {
            return ids;
        }
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            ids = new Ids(idProducer.getTraceId(), idProducer.getSpanId());
        } else {
            requestContext.activate();
            try {
                ids = new Ids(idProducer.getTraceId(), idProducer.getSpanId());
            } finally {
                requestContext.terminate();
            }
        }
        ctx.put(IDS_KEY, ids);
        return ids;
    }

    private record Ids(String traceId, String spanId) {
    }
}
