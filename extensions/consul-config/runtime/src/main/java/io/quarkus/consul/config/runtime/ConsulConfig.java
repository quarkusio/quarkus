package io.quarkus.consul.config.runtime;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "consul-config", phase = ConfigPhase.BOOTSTRAP)
public class ConsulConfig {

    /**
     * If set to true, the application will attempt to look up the configuration from Consul
     */
    @ConfigItem(defaultValue = "false")
    boolean enabled;

    /**
     * Consul agent related configuration
     */
    @ConfigItem
    AgentConfig agent;

    /**
     * Common prefix that all keys share when looking up the keys from Consul.
     * The prefix is <b>not</b> included in the keys of the user configuration
     */
    @ConfigItem
    Optional<String> prefix;

    /**
     * Keys whose value is a raw string.
     * When this is used, the keys that end up in the user configuration are the keys specified her with '/' replaced by '.'
     */
    @ConfigItem
    Optional<List<String>> rawValueKeys;

    /**
     * Keys whose value represents a properties file.
     * When this is used, the keys that end up in the user configuration are the keys of the properties file,
     * not these keys
     */
    @ConfigItem
    Optional<List<String>> propertiesValueKeys;

    /**
     * If set to true, the application will not start if any of the configured config sources cannot be located
     */
    @ConfigItem(defaultValue = "true")
    boolean failOnMissingKey;

    Map<String, ValueType> keysAsMap() {
        Map<String, ValueType> result = new LinkedHashMap<>();
        if (rawValueKeys.isPresent()) {
            for (String key : rawValueKeys.get()) {
                result.put(key, ValueType.RAW);
            }
        }
        if (propertiesValueKeys.isPresent()) {
            for (String key : propertiesValueKeys.get()) {
                result.put(key, ValueType.PROPERTIES);
            }
        }
        return result;
    }

    @ConfigGroup
    public static class AgentConfig {

        /**
         * Consul agent host
         */
        @ConfigItem(defaultValue = "localhost:8500")
        InetSocketAddress hostPort;

        /**
         * Whether or not to use HTTPS when communicating with the agent
         */
        @ConfigItem(defaultValue = "false")
        boolean useHttps;

        /**
         * Consul token to be provided when authentication is enabled
         */
        @ConfigItem
        Optional<String> token;

        /**
         * TrustStore to be used containing the SSL certificate used by Consul agent
         * Can be either a classpath resource or a file system path
         */
        @ConfigItem
        public Optional<Path> trustStore;

        /**
         * Password of TrustStore to be used containing the SSL certificate used by Consul agent
         */
        @ConfigItem
        public Optional<String> trustStorePassword;

        /**
         * KeyStore to be used containing the SSL certificate for authentication with Consul agent
         * Can be either a classpath resource or a file system path
         */
        @ConfigItem
        public Optional<Path> keyStore;

        /**
         * Password of KeyStore to be used containing the SSL certificate for authentication with Consul agent
         */
        @ConfigItem
        public Optional<String> keyStorePassword;

        /**
         * Password to recover key from KeyStore for SSL client authentication with Consul agent
         * If no value is provided, the key-store-password will be used
         */
        @ConfigItem
        public Optional<String> keyPassword;

        /**
         * When using HTTPS and no keyStore has been specified, whether or not to trust all certificates
         */
        @ConfigItem(defaultValue = "false")
        boolean trustCerts;

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out.
         * <p>
         * Specify `0` to wait indefinitely.
         */
        @ConfigItem(defaultValue = "10S")
        public Duration connectionTimeout;

        /**
         * The amount of time to wait for a read on a socket before an exception is thrown.
         * <p>
         * Specify `0` to wait indefinitely.
         */
        @ConfigItem(defaultValue = "60S")
        public Duration readTimeout;
    }

}
