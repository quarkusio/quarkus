package io.quarkus.azure.app.config.client.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BOOTSTRAP, name = AzureAppConfigClientConfig.NAME)
public class AzureAppConfigClientConfig {

    protected static final String NAME = "azure-app-config";

    /**
     * If enabled, will try to read the configuration from a Azure App Config Server
     */
    @ConfigItem
    public boolean enabled;

    /**
     * If set to true, the application will not stand up if it cannot obtain configuration from the Config Server
     */
    @ConfigItem
    public boolean failFast;

    /**
     * The Base URI where the Azure App Config Server is available
     */
    @ConfigItem
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
     * The credential to be used
     */
    @ConfigItem
    public String credential;

    /**
     * The secret to be used
     */
    @ConfigItem
    public String secret;

}
