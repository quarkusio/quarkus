package io.quarkus.vertx.http.runtime.cors;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigGroup
public class CORSConfig {

    /**
     * The origins allowed for CORS.
     *
     * A comma-separated list of valid URLs, such as `http://www.quarkus.io,http://localhost:3000`.
     * URLs enclosed in forward slashes are interpreted as regular expressions.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> origins = Optional.empty();

    /**
     * The HTTP methods allowed for CORS requests.
     *
     * A comma-separated list of valid HTTP methods, such as `GET,PUT,POST`.
     * If not set, the filter allows any HTTP method by default.
     *
     * Default: Any HTTP request method is allowed.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> methods = Optional.empty();

    /**
     * The HTTP headers allowed for CORS requests.
     *
     * A comma-separated list of valid headers, such as `X-Custom,Content-Disposition`.
     * If not set, the filter allows any header by default.
     *
     * Default: Any HTTP request header is allowed.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> headers = Optional.empty();

    /**
     * The HTTP headers exposed in CORS responses.
     *
     * A comma-separated list of headers to expose, such as `X-Custom,Content-Disposition`.
     *
     * Default: No headers are exposed.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> exposedHeaders = Optional.empty();

    /**
     * The `Access-Control-Max-Age` response header value in {@link java.time.Duration} format.
     *
     * Informs the browser how long it can cache the results of a preflight request.
     */
    @ConfigItem
    public Optional<Duration> accessControlMaxAge = Optional.empty();

    /**
     * The `Access-Control-Allow-Credentials` response header.
     *
     * Tells browsers if front-end JavaScript can be allowed to access credentials when the request's credentials mode,
     * `Request.credentials`, is set to `include`.
     *
     * Default: `true` if the `quarkus.http.cors.origins` property is set
     * and matches the precise `Origin` header value.
     */
    @ConfigItem
    public Optional<Boolean> accessControlAllowCredentials = Optional.empty();

    @Override
    public String toString() {
        return "CORSConfig{" +
                "origins=" + origins +
                ", methods=" + methods +
                ", headers=" + headers +
                ", exposedHeaders=" + exposedHeaders +
                ", accessControlMaxAge=" + accessControlMaxAge +
                ", accessControlAllowCredentials=" + accessControlAllowCredentials +
                '}';
    }
}
