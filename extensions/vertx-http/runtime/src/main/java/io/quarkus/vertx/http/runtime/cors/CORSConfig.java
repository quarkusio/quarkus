package io.quarkus.vertx.http.runtime.cors;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.vertx.core.http.HttpMethod;

@ConfigGroup
public class CORSConfig {

    /**
     * Origins allowed for CORS
     *
     * Comma separated list of valid URLs. ex: http://www.quarkus.io,http://localhost:3000
     * The filter allows any origin if this is not set.
     *
     * default: returns any requested origin as valid
     */
    @ConfigItem
    public Optional<List<String>> origins;

    /**
     * HTTP methods allowed for CORS
     *
     * Comma separated list of valid methods. ex: GET,PUT,POST
     * The filter allows any method if this is not set.
     *
     * default: returns any requested method as valid
     */
    @ConfigItem
    public Optional<List<HttpMethod>> methods;

    /**
     * HTTP headers allowed for CORS
     *
     * Comma separated list of valid headers. ex: X-Custom,Content-Disposition
     * The filter allows any header if this is not set.
     *
     * default: returns any requested header as valid
     */
    @ConfigItem
    public Optional<List<String>> headers;

    /**
     * HTTP headers exposed in CORS
     *
     * Comma separated list of valid headers. ex: X-Custom,Content-Disposition
     *
     * default: empty
     */
    @ConfigItem
    public Optional<List<String>> exposedHeaders;

    /**
     * The `Access-Control-Max-Age` response header value indicating
     * how long the results of a pre-flight request can be cached.
     */
    @ConfigItem
    public Optional<Duration> accessControlMaxAge;

    @Override
    public String toString() {
        return "CORSConfig{" +
                "origins=" + origins +
                ", methods=" + methods +
                ", headers=" + headers +
                ", exposedHeaders=" + exposedHeaders +
                ", accessControlMaxAge=" + accessControlMaxAge +
                '}';
    }
}
