package io.quarkus.spring.cloud.config.client.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    @WithConverter(PathConverter.class)
    Optional<Path> trustStore();

    /**
     * Password of TrustStore to be used containing the SSL certificate used by the Config server
     */
    Optional<String> trustStorePassword();

    /**
     * KeyStore to be used containing the SSL certificate for authentication with the Config server
     * Can be either a classpath resource or a file system path
     */
    @WithConverter(PathConverter.class)
    Optional<Path> keyStore();

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
    Map<String, String> headers();

    /**
     * The profiles to use for lookup
     */
    Optional<List<String>> profiles();

    /** */
    default boolean usernameAndPasswordSet() {
        return username().isPresent() && password().isPresent();
    }
}
