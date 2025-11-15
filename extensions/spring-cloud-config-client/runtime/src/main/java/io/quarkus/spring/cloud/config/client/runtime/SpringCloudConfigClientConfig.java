package io.quarkus.spring.cloud.config.client.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.runtime.configuration.PathConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.spring-cloud-config")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SpringCloudConfigClientConfig {
    /**
     * If enabled, will try to read the configuration from a Spring Cloud Config Server
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * If set to true, the application will not stand up if it cannot obtain configuration from the Config Server
     */
    @WithDefault("false")
    boolean failFast();

    /**
     * The Base URI where the Spring Cloud Config Server is available
     */
    @WithDefault("http://localhost:8888")
    String url();

    /**
     * Name of the application on Spring Cloud Config server.
     * Could be a list of names to load multiple files (value separated by a comma)
     */
    @WithDefault("${quarkus.application.name:}")
    String name();

    /**
     * The label to be used to pull remote configuration properties.
     * The default is set on the Spring Cloud Config Server
     * (generally "master" when the server uses a Git backend).
     */
    Optional<String> label();

    /**
     * The amount of time to wait when initially establishing a connection before giving up and timing out.
     * <p>
     * Specify `0` to wait indefinitely.
     */
    @WithDefault("10S")
    @WithConverter(DurationConverter.class)
    Duration connectionTimeout();

    /**
     * The amount of time to wait for a read on a socket before an exception is thrown.
     * <p>
     * Specify `0` to wait indefinitely.
     */
    @WithDefault("60S")
    @WithConverter(DurationConverter.class)
    Duration readTimeout();

    /**
     * The username to be used if the Config Server has BASIC Auth enabled
     */
    Optional<String> username();

    /**
     * The password to be used if the Config Server has BASIC Auth enabled
     */
    Optional<String> password();

    /**
     * TrustStore to be used containing the SSL certificate used by the Config server
     * Can be either a classpath resource or a file system path
     */
    Optional<@WithConverter(PathConverter.class) Path> trustStore();

    /**
     * Password of TrustStore to be used containing the SSL certificate used by the Config server
     */
    Optional<String> trustStorePassword();

    /**
     * KeyStore to be used containing the SSL certificate for authentication with the Config server
     * Can be either a classpath resource or a file system path
     */
    Optional<@WithConverter(PathConverter.class) Path> keyStore();

    /**
     * Password of KeyStore to be used containing the SSL certificate for authentication with the Config server
     */
    Optional<String> keyStorePassword();

    /**
     * Password to recover key from KeyStore for SSL client authentication with the Config server
     * If no value is provided, the key-store-password will be used
     */
    Optional<String> keyPassword();

    /**
     * When using HTTPS and no keyStore has been specified, whether to trust all certificates
     */
    @WithDefault("${quarkus.tls.trust-all:false}")
    boolean trustCerts();

    /**
     * Custom headers to pass the Spring Cloud Config Server when performing the HTTP request
     */
    @ConfigDocMapKey("header-name")
    Map<String, String> headers();

    /**
     * The profiles to use for lookup
     */
    Optional<List<String>> profiles();

    /**
     * Microprofile Config ordinal.
     */
    @WithDefault("450")
    int ordinal();

    /**
     * Configuration for Config Server discovery.
     */
    Optional<DiscoveryConfig> discovery();

    interface DiscoveryConfig {
        /**
         * Enable discovery of the Spring Cloud Config Server
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * The service ID to use when discovering the Spring Cloud Config Server
         */
        Optional<String> serviceId();

        /**
         * Eureka server configuration
         */
        Optional<EurekaConfig> eurekaConfig();

        interface EurekaConfig {
            /**
             * The service URL to use to specify Eureka server
             */
            Map<String, String> serviceUrl();

            /**
             * Indicates how often(in seconds) to fetch the registry information from the eureka server.
             */
            @WithDefault("30S")
            @WithConverter(DurationConverter.class)
            Duration registryFetchIntervalSeconds();
        }
    }

    /** */
    default boolean usernameAndPasswordSet() {
        return username().isPresent() && password().isPresent();
    }
}
