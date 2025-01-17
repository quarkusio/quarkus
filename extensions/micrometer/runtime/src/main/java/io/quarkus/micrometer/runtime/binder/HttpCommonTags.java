package io.quarkus.micrometer.runtime.binder;

import java.util.Objects;

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
     * @return the method tag
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
     * Falling back to {@code REDIRECTION} for 3xx responses if there wasn't a matched path pattern, {@code NOT_FOUND}
     * for 404 responses if there wasn't a matched path pattern, {@code root} for requests with no path info, and
     * {@code UNKNOWN}
     * for all other requests.
     *
     * @param pathInfo request path
     * @param initialPath initial path before request pattern matching took place. Pass in null if there is pattern matching
     *        done in the caller.
     * @param code status code of the response
     * @return the uri tag derived from the request
     */
    public static Tag uri(String pathInfo, String initialPath, int code, boolean suppress4xxErrors) {
        if (pathInfo == null) {
            return URI_UNKNOWN;
        }
        if (pathInfo.isEmpty() || "/".equals(pathInfo)) {
            return URI_ROOT;
        }

        if (code > 0) {
            if (code / 100 == 3) {
                if (isTemplatedPath(pathInfo, initialPath)) {
                    return Tag.of("uri", pathInfo);
                } else {
                    return URI_REDIRECTION;
                }
            } else if (code == 404) {
                if (isTemplatedPath(pathInfo, initialPath)) {
                    return Tag.of("uri", pathInfo);
                } else {
                    return URI_NOT_FOUND;
                }
            } else if (code >= 400) {
                if (!suppress4xxErrors) {
                    // legacy behaviour
                    return Tag.of("uri", pathInfo);
                } else if (isTemplatedPath(pathInfo, initialPath)) {
                    return Tag.of("uri", pathInfo);
                } else {
                    // Do not return the path info as it can lead to a metrics explosion
                    // for 4xx and 5xx responses
                    return URI_UNKNOWN;
                }
            }
        }

        // Use first segment of request path
        return Tag.of("uri", pathInfo);
    }

    private static boolean isTemplatedPath(String pathInfo, String initialPath) {
        // only include the path info if it has been matched to a template (initialPath != pathInfo) to avoid a metrics explosion with lots of entries
        return initialPath != null && !Objects.equals(initialPath, pathInfo);
    }
}
