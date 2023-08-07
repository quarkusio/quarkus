package io.quarkus.vertx.http.runtime.cors;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

public interface CORSConfig {
    /**
     * Origins allowed for CORS
     * <p>
     * Comma separated list of valid URLs, e.g.: http://www.quarkus.io,http://localhost:3000
     * In case an entry of the list is surrounded by forward slashes,
     * it is interpreted as a regular expression.
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> origins();

    /**
     * HTTP methods allowed for CORS
     * <p>
     * Comma separated list of valid methods. ex: GET,PUT,POST
     * The filter allows any method if this is not set.
     * <p>
     * default: returns any requested method as valid
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> methods();

    /**
     * HTTP headers allowed for CORS
     * <p>
     * Comma separated list of valid headers. ex: X-Custom,Content-Disposition
     * The filter allows any header if this is not set.
     * <p>
     * default: returns any requested header as valid
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> headers();

    /**
     * HTTP headers exposed in CORS
     * <p>
     * Comma separated list of valid headers. ex: X-Custom,Content-Disposition
     * <p>
     * default: empty
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> exposedHeaders();

    /**
     * The `Access-Control-Max-Age` response header value indicating
     * how long the results of a pre-flight request can be cached.
     */
    Optional<Duration> accessControlMaxAge();

    /**
     * The `Access-Control-Allow-Credentials` header is used to tell the
     * browsers to expose the response to front-end JavaScript code when
     * the request’s credentials mode Request.credentials is “include”.
     * <p>
     * The value of this header will default to `true` if `quarkus.http.cors.origins` property is set and
     * there is a match with the precise `Origin` header.
     */
    Optional<Boolean> accessControlAllowCredentials();
}
