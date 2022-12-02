package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.NormalizeRootHttpPathConverter;
import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;
import io.vertx.core.http.ClientAuth;

@ConfigRoot(name = "http", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HttpBuildTimeConfig {

    /**
     * The HTTP root path. All web content will be served relative to this root path.
     */
    @ConfigItem(defaultValue = "/")
    @ConvertWith(NormalizeRootHttpPathConverter.class)
    public String rootPath;

    public AuthConfig auth;

    /**
     * Configures the engine to require/request client authentication.
     * NONE, REQUEST, REQUIRED
     */
    @ConfigItem(name = "ssl.client-auth", defaultValue = "NONE")
    public ClientAuth tlsClientAuth;

    /**
     * If this is true then only a virtual channel will be set up for vertx web.
     * We have this switch for testing purposes.
     */
    @ConfigItem
    public boolean virtual;

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
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "q")
    public String nonApplicationRootPath;

    /**
     * The REST Assured client timeout for testing.
     */
    @ConfigItem(defaultValue = "30s")
    public Duration testTimeout;

    /**
     * If responses should be compressed.
     *
     * Note that this will attempt to compress all responses, to avoid compressing
     * already compressed content (such as images) you need to set the following header:
     *
     * Content-Encoding: identity
     *
     * Which will tell vert.x not to compress the response.
     */
    @ConfigItem
    public boolean enableCompression;

    /**
     * When enabled, vert.x will decompress the request's body if it's compressed.
     *
     * Note that the compression format (e.g., gzip) must be specified in the Content-Encoding header
     * in the request.
     */
    @ConfigItem
    public boolean enableDecompression;

    /**
     * List of media types for which the compression should be enabled automatically, unless declared explicitly via
     * {@link Compressed} or {@link Uncompressed}.
     */
    @ConfigItem(defaultValue = "text/html,text/plain,text/xml,text/css,text/javascript,application/javascript")
    public Optional<List<String>> compressMediaTypes;

    /**
     * The compression level used when compression support is enabled.
     */
    @ConfigItem
    public OptionalInt compressionLevel;
}
