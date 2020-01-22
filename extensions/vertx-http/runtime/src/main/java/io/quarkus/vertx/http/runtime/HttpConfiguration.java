package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class HttpConfiguration {

    /**
     * Enable the CORS filter.
     */
    @ConfigItem(name = "cors", defaultValue = "false")
    public boolean corsEnabled;

    /**
     * The HTTP port
     */
    @ConfigItem(defaultValue = "8080")
    public int port;

    /**
     * The HTTP port used to run tests
     */
    @ConfigItem(defaultValue = "8081")
    public int testPort;

    /**
     * The HTTP host
     */
    @ConfigItem(defaultValue = "0.0.0.0")
    public String host;

    /**
     * The HTTPS port
     */
    @ConfigItem(defaultValue = "8443")
    public int sslPort;

    /**
     * The HTTPS port used to run tests
     */
    @ConfigItem(defaultValue = "8444")
    public int testSslPort;

    /**
     * If this is true then the address, scheme etc will be set from headers forwarded by the proxy server, such as
     * {@code X-Forwarded-For}. This should only be set if you are behind a proxy that sets these headers.
     */
    @ConfigItem(defaultValue = "false")
    public boolean proxyAddressForwarding;

    /**
     * If this is true and proxy address forwarding is enabled then the standard {@code Forwarded} header will be used,
     * rather than the more common but not standard {@code X-Forwarded-For}.
     */
    @ConfigItem(defaultValue = "false")
    public boolean allowForwarded;

    /**
     * The CORS config
     */
    public CORSConfig cors;

    /**
     * The SSL config
     */
    public ServerSslConfig ssl;

    /**
     * The number if IO threads used to perform IO. This will be automatically set to a reasonable value based on
     * the number of CPU cores if it is not provided. If this is set to a higher value than the number of Vert.x event
     * loops then it will be capped at the number of event loops.
     *
     * In general this should be controlled by setting quarkus.vertx.event-loops-pool-size, this setting should only
     * be used if you want to limit the number of HTTP io threads to a smaller number than the total number of IO threads.
     */
    @ConfigItem
    public OptionalInt ioThreads;

    /**
     * Server limits configuration
     */
    public ServerLimitsConfig limits;

    /**
     * Http connection idle timeout
     */
    @ConfigItem(defaultValue = "30M", name = "idle-timeout")
    public Duration idleTimeout;

    /**
     * Request body related settings
     */
    public BodyConfig body;

    /**
     * The encryption key that is used to store persistent logins (e.g. for form auth). Logins are stored in a persistent
     * cookie that is encrypted with AES-256 using a key derived from a SHA-256 hash of the key that is provided here.
     *
     * If no key is provided then an in-memory one will be generated, this will change on every restart though so it
     * is not suitable for production environments. This must be more than 16 characters long for security reasons
     */
    @ConfigItem(name = "auth.session.encryption-key")
    public Optional<String> encryptionKey;

    public int determinePort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testPort : port;
    }

    public int determineSslPort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testSslPort : sslPort;
    }

}
