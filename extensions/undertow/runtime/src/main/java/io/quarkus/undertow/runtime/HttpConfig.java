package io.quarkus.undertow.runtime;

import java.util.OptionalInt;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.ssl.ServerSslConfig;
import io.quarkus.undertow.runtime.filters.CORSConfig;

/**
 * Configuration which applies to the HTTP server.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class HttpConfig {

    /**
     * The HTTP port
     */
    @ConfigItem(defaultValue = "8080")
    public int port;

    /**
     * The HTTPS port
     */
    @ConfigItem(defaultValue = "8443")
    public int sslPort;

    /**
     * The HTTP port used to run tests
     */
    @ConfigItem(defaultValue = "8081")
    public int testPort;

    /**
     * The HTTPS port used to run tests
     */
    @ConfigItem(defaultValue = "8444")
    public int testSslPort;
    /**
     * The HTTP host
     */
    @ConfigItem(defaultValue = "0.0.0.0")
    public String host;

    /**
     * The number if IO threads used to perform IO. This will be automatically set to a reasonable value based on
     * the number of CPU cores if it is not provided
     */
    @ConfigItem
    public OptionalInt ioThreads;

    /**
     * The SSL config
     */
    public ServerSslConfig ssl;

    /**
     * The CORS config
     */
    public CORSConfig cors;

    public int determinePort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testPort : port;
    }

    public int determineSslPort(LaunchMode launchMode) {
        return launchMode == LaunchMode.TEST ? testSslPort : sslPort;
    }

}
