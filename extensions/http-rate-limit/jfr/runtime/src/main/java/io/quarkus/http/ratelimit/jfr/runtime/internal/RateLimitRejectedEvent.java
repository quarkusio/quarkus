package io.quarkus.http.ratelimit.jfr.runtime.internal;

import io.quarkus.jfr.api.SpanIdRelational;
import io.quarkus.jfr.api.TraceIdRelational;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;

/**
 * Never records the key or the client address: keys can be addresses, API keys or token hashes, and recordings are
 * routinely shared.
 */
@Label("Rate Limit Rejected")
@Category({ "Quarkus", "HTTP" })
@Name("quarkus.RateLimitRejected")
@Description("A rate limit rule rejected the request with 429")
@StackTrace(false)
public class RateLimitRejectedEvent extends Event {

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

    @Label("HTTP Method")
    @Description("HTTP method of the request")
    String httpMethod;

    @Label("Path")
    @Description("Canonical path of the request, the one the rules were matched against")
    String path;

    @Label("Retry After")
    @Description("Value of the Retry-After header")
    @Timespan(Timespan.SECONDS)
    long retryAfter;
}
