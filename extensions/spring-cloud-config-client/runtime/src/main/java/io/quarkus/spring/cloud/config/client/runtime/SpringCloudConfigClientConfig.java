package io.quarkus.spring.cloud.config.client.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BOOTSTRAP, name = SpringCloudConfigClientConfig.NAME)
public class SpringCloudConfigClientConfig {

    protected static final String NAME = "spring-cloud-config";

    /**
     * If enabled, will try to read the configuration from a Spring Cloud Config Server
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * If set to true, the application will not stand up if it cannot obtain configuration from the Config Server
     */
    @ConfigItem(defaultValue = "false")
    public boolean failFast;

    /**
     * The Base URI where the Spring Cloud Config Server is available
     */
    @ConfigItem(defaultValue = "http://localhost:8888")
    public String url;

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

    public boolean usernameAndPasswordSet() {
        return username.isPresent() && password.isPresent();
    }
}
