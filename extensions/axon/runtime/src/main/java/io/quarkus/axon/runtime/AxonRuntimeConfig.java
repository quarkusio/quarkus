package io.quarkus.axon.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * The Axon configuration.
 */
@ConfigRoot(name = "axon", phase = ConfigPhase.RUN_TIME)
public class AxonRuntimeConfig {

    /**
     * Autostart the Axon client on start of the application.
     *
     * Default: true
     */
    @ConfigItem
    public boolean autostart = true;

    /**
     * The id of the Axon client
     */
    @ConfigItem
    public Optional<String> clientId;

    /**
     * The name of the component
     */
    @ConfigItem
    public Optional<String> componentName;

    /**
     * The Axon server locations or locations
     * For example: 'localhost:1234, 10.12.14.1:4321'
     *
     * Default: localhost (will also use default port 8124)
     */
    @ConfigItem
    public Optional<String> servers;

    /**
     * Enable SSL. Requires also a certFile
     */
    @ConfigItem
    public boolean sslEnabled;

    /**
     * The location of the SSL certFile
     */
    @ConfigItem
    public Optional<String> certFile;

    /**
     * token
     */
    @ConfigItem
    public Optional<String> token;

    /**
     * eventSecretKey
     */
    @ConfigItem
    public Optional<String> eventSecretKey;

    /**
     * context
     */
    @ConfigItem
    public Optional<String> context;

    /**
     * maxMessageSize
     */
    @ConfigItem
    public Optional<Integer> maxMessageSize;

    /**
     * snapshotPrefetch
     */
    @ConfigItem
    public Optional<Integer> snapshotPrefetch;

}
