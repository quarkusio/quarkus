package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.NormalizeRootHttpPathConverter;
import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.vertx.core.http.ClientAuth;

@ConfigMapping(prefix = "quarkus.http")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface HttpBuildTimeConfig {
    /**
     * The HTTP root path. All web content will be served relative to this root path.
     */
    @WithDefault("/")
    @WithConverter(NormalizeRootHttpPathConverter.class)
    String rootPath();

    /**
     * Authentication configuration.
     */
    AuthConfig auth();

    /**
     * Configures the engine to require/request client authentication.
     * NONE, REQUEST, REQUIRED
     */
    @WithName("ssl.client-auth")
    @WithDefault("NONE")
    ClientAuth tlsClientAuth();

    /**
     * If this is true then only a virtual channel will be set up for vertx web.
     * We have this switch for testing purposes.
     */
    @WithDefault("false")
    boolean virtual();

    /**
     * A common root path for non-application endpoints. Various extension-provided endpoints such as metrics, health,
     * and openapi are deployed under this path by default.
     *
     * * Relative path (Default, `q`) ->
     * Non-application endpoints will be served from
     * `${quarkus.http.root-path}/${quarkus.http.non-application-root-path}`.
     * * Absolute path (`/q`) ->
     * Non-application endpoints will be served from the specified path.
     * * `${quarkus.http.root-path}` -> Setting this path to the same value as HTTP root path disables
     * this root path. All extension-provided endpoints will be served from `${quarkus.http.root-path}`.
     *
     * If the management interface is enabled, the root path for the endpoints exposed on the management interface
     * is configured using the `quarkus.management.root-path` property instead of this property.
     *
     * @asciidoclet
     */
    @WithDefault("q")
    String nonApplicationRootPath();

    /**
     * The REST Assured client timeout for testing.
     */
    @WithDefault("30s")
    Duration testTimeout();

    /**
     * If enabled then the response body is compressed if the {@code Content-Type} header is set and the value is a compressed
     * media type as configured via {@link #compressMediaTypes}.
     *
     * Note that the RESTEasy Reactive and Reactive Routes extensions also make it possible to enable/disable compression
     * declaratively using the annotations {@link io.quarkus.vertx.http.Compressed} and
     * {@link io.quarkus.vertx.http.Uncompressed}.
     */
    @WithDefault("false")
    boolean enableCompression();

    /**
     * When enabled, vert.x will decompress the request's body if it's compressed.
     *
     * Note that the compression format (e.g., gzip) must be specified in the Content-Encoding header
     * in the request.
     */
    @WithDefault("false")
    boolean enableDecompression();

    /**
     * List of media types for which the compression should be enabled automatically, unless declared explicitly via
     * {@link Compressed} or {@link Uncompressed}.
     */
    @WithDefault("text/html,text/plain,text/xml,text/css,text/javascript,application/javascript")
    Optional<List<String>> compressMediaTypes();

    /**
     * The compression level used when compression support is enabled.
     */
    OptionalInt compressionLevel();
}
