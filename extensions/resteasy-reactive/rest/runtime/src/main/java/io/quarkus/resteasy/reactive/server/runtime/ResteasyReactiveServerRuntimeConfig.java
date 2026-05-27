package io.quarkus.resteasy.reactive.server.runtime;

import java.nio.charset.Charset;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.rest")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ResteasyReactiveServerRuntimeConfig {

    /**
     * Logging configuration.
     */
    LoggingConfig logging();

    interface LoggingConfig {

        /**
         * Scope of logging for the server.
         * <br/>
         * The possible values are:
         * <ul>
         * <li>{@code request-response} - enables logging of incoming requests and outgoing responses,
         * including headers and (optionally truncated) body</li>
         * <li>{@code none} - no additional logging</li>
         * </ul>
         */
        @WithDefault("none")
        String scope();

        /**
         * Whether to log the request and response body.
         * <p>
         * Disabled by default to avoid accidentally logging sensitive data.
         */
        @WithDefault("false")
        boolean includeBody();

        /**
         * How many characters of the body should be logged. Message body can be large and can easily pollute the logs.
         * <p>
         * Truncates text content on both the request and response side: plain text bodies (e.g. JSON, XML),
         * and each individual text part of a multipart body.
         * <p>
         * An incoming multipart request is only buffered for logging if its {@code Content-Length} is known
         * and does not exceed a threshold derived from this limit (clamped between a small internal floor
         * and ceiling, in bytes); otherwise a short note is logged instead of the body. See the
         * request/response logging section of the Quarkus REST guide for details.
         * <p>
         * By default, set to 100.
         */
        @WithDefault("100")
        int bodyLimit();

        /**
         * Which request and response header values to mask in logs.
         * <p>
         * The value of any matching header will be replaced with {@code "<hidden>"}.
         * The header name itself remains visible. E.g. {@code Authorization: <hidden>}
         */
        @WithDefault("Authorization,Cookie")
        Set<String> maskedHeaders();
    }

    /**
     * Input part configuration.
     */
    MultipartConfigGroup multipart();

    interface MultipartConfigGroup {

        /**
         * Input part configuration.
         */
        InputPartConfigGroup inputPart();
    }

    interface InputPartConfigGroup {

        /**
         * Default charset.
         */
        @WithDefault("UTF-8")
        Charset defaultCharset();
    }
}
