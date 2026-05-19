package io.quarkus.vertx.http.runtime;

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
public interface VertxHttpBuildTimeConfig {
    /**
     * The HTTP root path. All web content will be served relative to this root path.
     */
    @WithDefault("/")
    @WithConverter(NormalizeRootHttpPathConverter.class)
    String rootPath();

    /**
     * Authentication mechanism and SecurityRealm name information used for configuring HTTP auth instance for the
     * deployment.
     */
    AuthConfig auth();

    /**
     * Configures the engine to require/request client authentication.
     * {@code NONE, REQUEST, REQUIRED}.
     * <p>
     * When set to {@code REQUIRED}, it's recommended to also set `quarkus.http.insecure-requests=disabled` to disable the
     * plain HTTP port. If `quarkus.http.insecure-requests` is not set, but this parameter is set to {@code REQUIRED}, then,
     * `quarkus.http.insecure-requests` is automatically set to `disabled`.
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
     */
    @WithDefault("q")
    String nonApplicationRootPath();

    /**
     * If enabled then the response body is compressed if the {@code Content-Type} header is set and the value is a compressed
     * media type as configured via {@link #compressMediaTypes}.
     * <p>
     * Note that the RESTEasy Reactive and Reactive Routes extensions also make it possible to enable/disable compression
     * declaratively using the annotations {@link io.quarkus.vertx.http.Compressed} and
     * {@link io.quarkus.vertx.http.Uncompressed}.
     */
    @WithDefault("false")
    boolean enableCompression();

    /**
     * When enabled, Vert.x installs Netty's {@code HttpContentDecompressor} so request bodies may be
     * decompressed before they reach application code, based on the {@code Content-Encoding} header.
     * <p>
     * Supported codings match Netty on the JVM (and therefore depend on optional native libraries for {@code br}
     * and {@code zstd}): {@code gzip} / {@code x-gzip}, {@code deflate} / {@code x-deflate}, and {@code br} when Brotli
     * is available. On the JVM, inbound {@code snappy} uses Netty's Snappy framing format (as produced by Netty's
     * {@code SnappyFrameEncoder}, not arbitrary raw Snappy blocks). On the JVM, {@code zstd} may also be supported when
     * Netty's Zstd support is available.
     * <p>
     * When this flag is {@code false} (the default), the body bytes are passed through unchanged and
     * {@code Content-Encoding} is not interpreted for inbound decompression.
     * <p>
     * Unrecognized codings are passed through without decompression (they do not automatically produce an HTTP error
     * response). When decompression fails for a declared coding (invalid compressed payload, malformed framing, and so
     * on), Vert.x / Netty may close the connection without a normal application-level HTTP response; details depend on
     * HTTP version and timing in the inbound pipeline.
     * <p>
     * GraalVM native executables use substitutions for Netty's inbound decompressor that only wire {@code gzip},
     * {@code deflate}, and {@code br} (when Brotli loads). Inbound {@code snappy} and {@code zstd} are not decoded in
     * native mode even when this option is {@code true} (the body is passed through unchanged while the
     * {@code Content-Encoding} header may still be set).
     */
    @WithDefault("false")
    boolean enableDecompression();

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
    @WithDefault("gzip,deflate")
    Optional<List<String>> compressors();

    /**
     * List of media types for which the compression should be enabled automatically, unless declared explicitly via
     * {@link Compressed} or {@link Uncompressed}.
     */
    @WithDefault("text/html,text/plain,text/xml,text/css,text/javascript,application/javascript,application/json,application/graphql+json,application/xhtml+xml")
    Optional<List<String>> compressMediaTypes();

    /**
     * The compression level used when compression support is enabled.
     */
    OptionalInt compressionLevel();

    /**
     * Configure Quarkus to serve static files from a local filesystem directory (outside of Java resources)
     *
     */
    @WithName("static-dir")
    Optional<HttpStaticDirConfig> httpStaticDirConfig();
}
