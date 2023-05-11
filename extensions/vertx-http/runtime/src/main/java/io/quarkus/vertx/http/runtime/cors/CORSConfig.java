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
     * Origins allowed for CORS
     *
     * Comma separated list of valid URLs, e.g.: http://www.quarkus.io,http://localhost:3000
     * In case an entry of the list is surrounded by forward slashes,
     * it is interpreted as a regular expression.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> origins = Optional.empty();

    /**
     * HTTP methods allowed for CORS
     *
     * Comma separated list of valid methods. ex: GET,PUT,POST
     * The filter allows any method if this is not set.
     *
     * default: returns any requested method as valid
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> methods = Optional.empty();

    /**
     * HTTP headers allowed for CORS
     *
     * Comma separated list of valid headers. ex: X-Custom,Content-Disposition
     * The filter allows any header if this is not set.
     *
     * default: returns any requested header as valid
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> headers = Optional.empty();

    /**
     * HTTP headers exposed in CORS
     *
     * Comma separated list of valid headers. ex: X-Custom,Content-Disposition
     *
     * default: empty
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> exposedHeaders = Optional.empty();

    /**
     * The `Access-Control-Max-Age` response header value indicating
     * how long the results of a pre-flight request can be cached.
     */
    @ConfigItem
    public Optional<Duration> accessControlMaxAge = Optional.empty();

    /**
     * The `Access-Control-Allow-Credentials` header is used to tell the
     * browsers to expose the response to front-end JavaScript code when
     * the request’s credentials mode Request.credentials is “include”.
     *
     * The value of this header will default to `true` if `quarkus.http.cors.origins` property is set and
     * there is a match with the precise `Origin` header.
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
