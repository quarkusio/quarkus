package io.quarkus.micrometer.runtime.binder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.http.Outcome;

public class HttpCommonTags {
    private static final Logger log = Logger.getLogger(HttpCommonTags.class);
    private static final int MAX_HTTP_METHODS = 32;

    // Standard RFC 9110/5789/9512 methods, mapped to their pre-built tag to avoid creating a tag per request
    private static final Map<String, Tag> allowedHttpMethods = new HashMap<>(16);

    static {
        addAllowedHttpMethod("GET");
        addAllowedHttpMethod("HEAD");
        addAllowedHttpMethod("POST");
        addAllowedHttpMethod("PUT");
        addAllowedHttpMethod("DELETE");
        addAllowedHttpMethod("CONNECT");
        addAllowedHttpMethod("OPTIONS");
        addAllowedHttpMethod("TRACE");
        addAllowedHttpMethod("PATCH");
        addAllowedHttpMethod("QUERY");
    }

    // Pre-built status tags for the standard status code range, to avoid creating a tag per request
    private static final int MIN_CACHED_STATUS = 100;
    private static final int MAX_CACHED_STATUS = 599;
    private static final Tag[] STATUS_TAGS = new Tag[MAX_CACHED_STATUS - MIN_CACHED_STATUS + 1];

    static {
        for (int i = 0; i < STATUS_TAGS.length; i++) {
            STATUS_TAGS[i] = Tag.of("status", Integer.toString(MIN_CACHED_STATUS + i));
        }
    }

    private static void addAllowedHttpMethod(String method) {
        allowedHttpMethods.put(method, Tag.of("method", method));
    }

    public static void setAdditionalHttpMethods(List<String> additionalMethods) {
        if (additionalMethods != null) {
            for (String method : additionalMethods) {
                if (allowedHttpMethods.size() >= MAX_HTTP_METHODS) {
                    log.warnf("Maximum number of allowed HTTP methods (%d) reached. Ignoring remaining entries.",
                            MAX_HTTP_METHODS);
                    break;
                }
                addAllowedHttpMethod(method.toUpperCase());
            }
        }
    }

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
        if (method == null) {
            return METHOD_UNKNOWN;
        }
        Tag tag = allowedHttpMethods.get(method);
        return tag != null ? tag : METHOD_UNKNOWN;
    }

    /**
     * Creates a {@code status} tag based on the status of the given {@code response code}.
     *
     * @param statusCode the HTTP response code
     * @return the status tag derived from the status of the response
     */
    public static Tag status(int statusCode) {
        if (statusCode >= MIN_CACHED_STATUS && statusCode <= MAX_CACHED_STATUS) {
            return STATUS_TAGS[statusCode - MIN_CACHED_STATUS];
        }
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

        return Tag.of("uri", pathInfo);
    }

    public static String address(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return "unknown";
        }
        int port = uri.getPort();
        return port > 0 ? host + ":" + port : host;
    }

    private static boolean isTemplatedPath(String pathInfo, String initialPath) {
        // only include the path info if it has been matched to a template (initialPath != pathInfo) to avoid a metrics explosion with lots of entries
        // /not-there/ must have the same behaviour as /not-there
        return initialPath != null && !(Objects.equals(initialPath, pathInfo) ||
                Objects.equals(initialPath, pathInfo + "/"));
    }
}
