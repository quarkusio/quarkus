package io.quarkus.micrometer.runtime.binder;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.http.Outcome;

public class HttpCommonTags {
    public static final Tag URI_NOT_FOUND = Tag.of("uri", "NOT_FOUND");
    public static final Tag URI_REDIRECTION = Tag.of("uri", "REDIRECTION");
    public static final Tag URI_ROOT = Tag.of("uri", "root");
    static final Tag URI_UNKNOWN = Tag.of("uri", "UNKNOWN");

    static final Tag STATUS_UNKNOWN = Tag.of("status", "UNKNOWN");
    public static final Tag STATUS_RESET = Tag.of("status", "RESET");

    public static final Tag METHOD_UNKNOWN = Tag.of("method", "UNKNOWN");

    /**
     * Creates an {@code method} {@code Tag} derived from the given {@code HTTP method}.
     *
     * @param method the HTTP method
     * @return the outcome tag
     */
    public static Tag method(String method) {
        return method == null ? METHOD_UNKNOWN : Tag.of("method", method);
    }

    /**
     * Creates a {@code status} tag based on the status of the given {@code response code}.
     *
     * @param statusCode the HTTP response code
     * @return the status tag derived from the status of the response
     */
    public static Tag status(int statusCode) {
        return (statusCode > 0) ? Tag.of("status", Integer.toString(statusCode)) : STATUS_UNKNOWN;
    }

    /**
     * Creates an {@code outcome} {@code Tag} derived from the given {@code response code}.
     *
     * @param statusCode the HTTP response code
     * @return the outcome tag
     */
    public static Tag outcome(int statusCode) {
        return Outcome.forStatus(statusCode).asTag();
    }

    /**
     * Creates a {@code uri} tag based on the URI of the given {@code request}.
     * Falling back to {@code REDIRECTION} for 3xx responses, {@code NOT_FOUND}
     * for 404 responses, {@code root} for requests with no path info, and {@code UNKNOWN}
     * for all other requests.
     *
     *
     * @param pathInfo
     * @param code status code of the response
     * @return the uri tag derived from the request
     */
    public static Tag uri(String pathInfo, int code) {
        if (code > 0) {
            if (code / 100 == 3) {
                return URI_REDIRECTION;
            } else if (code == 404) {
                return URI_NOT_FOUND;
            }
        }
        if (pathInfo == null) {
            return URI_UNKNOWN;
        }
        if (pathInfo.isEmpty() || "/".equals(pathInfo)) {
            return URI_ROOT;
        }

        // Use first segment of request path
        return Tag.of("uri", pathInfo);
    }
}
