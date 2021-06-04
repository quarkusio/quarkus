package io.quarkus.spring.cloud.config.client.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BOOTSTRAP, name = SpringCloudConfigClientConfig.NAME)
public class SpringCloudConfigClientConfig {

    public static final String NAME = "spring-cloud-config";

    /**
     * If enabled, will try to read the configuration from a Spring Cloud Config Server
     */
    @ConfigItem
    public boolean enabled;

    /**
     * If set to true, the application will not stand up if it cannot obtain configuration from the Config Server
     */
    @ConfigItem
    public boolean failFast;

    /**
     * The Base URI where the Spring Cloud Config Server is available
     */
    @ConfigItem(defaultValue = "http://localhost:8888")
    public String url;

    /**
     * The label to be used to pull remote configuration properties.
     * The default is set on the Spring Cloud Config Server
     * (generally "master" when the server uses a Git backend).
     */
    @ConfigItem
    public Optional<String> label;

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

    /**
     * The username to be used if the Config Server has BASIC Auth enabled
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The password to be used if the Config Server has BASIC Auth enabled
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * TrustStore to be used containing the SSL certificate used by the Config server
     * Can be either a classpath resource or a file system path
     */
    @ConfigItem
    public Optional<Path> trustStore;

    /**
     * Password of TrustStore to be used containing the SSL certificate used by the Config server
     */
    @ConfigItem
    public Optional<String> trustStorePassword;

    /**
     * KeyStore to be used containing the SSL certificate for authentication with the Config server
     * Can be either a classpath resource or a file system path
     */
    @ConfigItem
    public Optional<Path> keyStore;

    /**
     * Password of KeyStore to be used containing the SSL certificate for authentication with the Config server
     */
    @ConfigItem
    public Optional<String> keyStorePassword;

    /**
     * Password to recover key from KeyStore for SSL client authentication with the Config server
     * If no value is provided, the key-store-password will be used
     */
    @ConfigItem
    public Optional<String> keyPassword;

    /**
     * When using HTTPS and no keyStore has been specified, whether or not to trust all certificates
     */
    @ConfigItem(defaultValue = "false")
    public boolean trustCerts;

    /**
     * Custom headers to pass the Spring Cloud Config Server when performing the HTTP request
     */
    @ConfigItem
    public Map<String, String> headers;

    /**
     * The OIDC client to be used if the Config Server has OIDC enabled
     */
    @ConfigItem
    public OidcClientConfig oidc;

    public boolean usernameAndPasswordSet() {
        return username.isPresent() && password.isPresent();
    }
}
