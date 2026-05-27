package io.quarkus.resteasy.reactive.server.runtime;

import java.nio.charset.Charset;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;
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

        /**
         * Maximum body size to buffer when logging a multipart request body.
         * <p>
         * Applies only when the request {@code Content-Length} is known and does not exceed this limit.
         * Requests with {@code Transfer-Encoding: chunked}, no {@code Content-Length}, or a
         * {@code Content-Length} above this limit are not buffered — a short note is logged instead.
         * <p>
         * Supports size units: {@code k} / {@code K} for kibibytes, {@code m} / {@code M} for mebibytes.
         * For example, {@code 10k} means 10 KiB.
         * <p>
         * By default, set to 10k (10 KiB).
         */
        @WithDefault("10k")
        MemorySize bodyBufferLimit();
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
