package io.quarkus.http.ratelimit.jfr.runtime.internal;

import io.quarkus.jfr.api.SpanIdRelational;
import io.quarkus.jfr.api.TraceIdRelational;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import jdk.jfr.Threshold;

/**
 * Spans one rule's decision for one request: key resolution, limitResolver and the store call. Never records the key or
 * the client address.
 */
@Label("Rate Limit Evaluation")
@Category({ "Quarkus", "HTTP" })
@Name("quarkus.RateLimitEvaluation")
@Description("A rate limit rule decided on a request during this period")
@StackTrace(false)
@Threshold("10 ms")
public class RateLimitEvaluationEvent extends Event {

    @Label("Trace ID")
    @Description("Trace ID to identify the request")
    @TraceIdRelational
    String traceId;

    @Label("Span ID")
    @Description("Span ID to identify the request if necessary")
    @SpanIdRelational
    String spanId;

    @Label("Rule")
    @Description("Name of the rate limit rule")
    String rule;

    @Label("Phase")
    @Description("pre-authentication or post-authentication")
    String phase;

    @Label("Store")
    @Description("Name of the store of the rule")
    String store;

    @Label("Outcome")
    @Description("ALLOWED, REJECTED, UNAVAILABLE, or SKIPPED")
    String outcome;
}
