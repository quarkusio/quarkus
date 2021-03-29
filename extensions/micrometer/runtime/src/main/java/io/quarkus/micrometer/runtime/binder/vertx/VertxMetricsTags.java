package io.quarkus.micrometer.runtime.binder.vertx;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.http.Outcome;
import io.quarkus.micrometer.runtime.binder.HttpCommonTags;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class VertxMetricsTags {
    private static final Logger log = Logger.getLogger(VertxMetricsTags.class);

    /**
     * Creates a {@code method} tag based on the {@link HttpServerRequest#method()}
     * method} of the given {@code request}.
     *
     * @param method the request method
     * @return the method tag whose value is a capitalized method (e.g. GET).
     */
    public static Tag method(HttpMethod method) {
        return (method != null) ? Tag.of("method", method.toString()) : HttpCommonTags.METHOD_UNKNOWN;
    }

    /**
     * Creates an {@code outcome} {@code Tag} derived from the given {@code response}.
     *
     * @param response the response
     * @return the outcome tag
     */
    public static Tag outcome(HttpServerResponse response) {
        if (response != null) {
            return Outcome.forStatus(response.getStatusCode()).asTag();
        }
        return Outcome.UNKNOWN.asTag();
    }

}
