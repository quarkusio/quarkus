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
     * {@code NONE, REQUEST, REQUIRED}.
     * <p>
     * When set to {@code REQUIRED}, it's recommended to also set `quarkus.http.insecure-requests=disabled` to disable the
     * plain HTTP port. If `quarkus.http.insecure-requests` is not set, but this parameter is set to {@code REQUIRED}, then,
     * `quarkus.http.insecure-requests` is automatically set to `disabled`.
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
     * <p>
     * * Relative path (Default, `q`) ->
     * Non-application endpoints will be served from
     * `${quarkus.http.root-path}/${quarkus.http.non-application-root-path}`.
     * * Absolute path (`/q`) ->
     * Non-application endpoints will be served from the specified path.
     * * `${quarkus.http.root-path}` -> Setting this path to the same value as HTTP root path disables
     * this root path. All extension-provided endpoints will be served from `${quarkus.http.root-path}`.
     * <p>
     * If the management interface is enabled, the root path for the endpoints exposed on the management interface
     * is configured using the `quarkus.management.root-path` property instead of this property.
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
     * If enabled then the response body is compressed if the {@code Content-Type} header is set and the value is a compressed
     * media type as configured via {@link #compressMediaTypes}.
     * <p>
     * Note that the RESTEasy Reactive and Reactive Routes extensions also make it possible to enable/disable compression
     * declaratively using the annotations {@link io.quarkus.vertx.http.Compressed} and
     * {@link io.quarkus.vertx.http.Uncompressed}.
     */
    @ConfigItem
    public boolean enableCompression;

    /**
     * When enabled, vert.x will decompress the request's body if it's compressed.
     * <p>
     * Note that the compression format (e.g., gzip) must be specified in the Content-Encoding header
     * in the request.
     */
    @ConfigItem
    public boolean enableDecompression;

    /**
     * If user adds br, then brotli will be added to the list of supported compression algorithms.
     * It implies loading libbrotli native library via JNI and in case of native image,
     * packing the native library into the native image as a resource thus inflating its size.
     * Note that a native shared object library must be available for your platform in Brotli4J project.
     * <p>
     * Client expresses its capability by sending Accept-Encoding header, e.g.
     * Accept-Encoding: deflate, gzip, br
     * Server chooses the compression algorithm based on the client's capabilities and
     * marks it in a response header, e.g.:
     * content-encoding: gzip
     *
     */
    @ConfigItem(defaultValue = "gzip,deflate")
    public Optional<List<String>> compressors;

    /**
     * List of media types for which the compression should be enabled automatically, unless declared explicitly via
     * {@link Compressed} or {@link Uncompressed}.
     */
    @ConfigItem(defaultValue = "text/html,text/plain,text/xml,text/css,text/javascript,application/javascript,application/json,application/graphql+json,application/xhtml+xml")
    public Optional<List<String>> compressMediaTypes;

    /**
     * The compression level used when compression support is enabled.
     */
    @ConfigItem
    public OptionalInt compressionLevel;
}
