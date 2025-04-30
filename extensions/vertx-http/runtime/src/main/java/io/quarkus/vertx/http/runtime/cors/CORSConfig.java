package io.quarkus.vertx.http.runtime.cors;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

public interface CORSConfig {
    /**
     * The origins allowed for CORS.
     * <p>
     * A comma-separated list of valid URLs, such as `http://www.quarkus.io,http://localhost:3000`.
     * URLs enclosed in forward slashes are interpreted as regular expressions.
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> origins();

    /**
     * The HTTP methods allowed for CORS requests.
     * <p>
     * A comma-separated list of valid HTTP methods, such as `GET,PUT,POST`.
     * If not set, the filter allows any HTTP method by default.
     * <p>
     * Default: Any HTTP request method is allowed.
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> methods();

    /**
     * The HTTP headers allowed for CORS requests.
     * <p>
     * A comma-separated list of valid headers, such as `X-Custom,Content-Disposition`.
     * If not set, the filter allows any header by default.
     * <p>
     * Default: Any HTTP request header is allowed.
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> headers();

    /**
     * The HTTP headers exposed in CORS responses.
     * <p>
     * A comma-separated list of headers to expose, such as `X-Custom,Content-Disposition`.
     * <p>
     * Default: No headers are exposed.
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> exposedHeaders();

    /**
     * The `Access-Control-Max-Age` response header value in {@link java.time.Duration} format.
     * <p>
     * Informs the browser how long it can cache the results of a preflight request.
     */
    Optional<Duration> accessControlMaxAge();

    /**
     * The `Access-Control-Allow-Credentials` response header.
     * <p>
     * Tells browsers if front-end JavaScript can be allowed to access credentials when the request's credentials mode,
     * `Request.credentials`, is set to `include`.
     * <p>
     * Default: `true` if the `quarkus.http.cors.origins` property is set
     * and matches the precise `Origin` header value.
     */
    Optional<Boolean> accessControlAllowCredentials();
}
