package io.quarkus.vertx.http.runtime.management;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.vertx.core.http.ClientAuth;

/**
 * Management interface configuration.
 */
@ConfigRoot(name = "management", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class ManagementInterfaceBuildTimeConfig {

    /**
     * Enables / Disables the usage of a separate interface/port to expose the management endpoints.
     * If sets to {@code true}, the management endpoints will be exposed to a different HTTP server.
     * This avoids exposing the management endpoints on a publicly available server.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * Authentication configuration
     */
    public ManagementAuthConfig auth;

    /**
     * Configures the engine to require/request client authentication.
     * NONE, REQUEST, REQUIRED
     */
    @ConfigItem(name = "ssl.client-auth", defaultValue = "NONE")
    public ClientAuth tlsClientAuth;

    /**
     * A common root path for management endpoints. Various extension-provided management endpoints such as metrics
     * and health are deployed under this path by default.
     */
    @ConfigItem(defaultValue = "/q")
    public String rootPath;

    /**
     * If responses should be compressed.
     * <p>
     * Note that this will attempt to compress all responses, to avoid compressing
     * already compressed content (such as images) you need to set the following header:
     * <p>
     * Content-Encoding: identity
     * <p>
     * Which will tell vert.x not to compress the response.
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
     * The compression level used when compression support is enabled.
     */
    @ConfigItem
    public OptionalInt compressionLevel;
}
