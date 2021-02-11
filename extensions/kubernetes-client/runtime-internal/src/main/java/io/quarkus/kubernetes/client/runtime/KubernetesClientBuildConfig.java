package io.quarkus.kubernetes.client.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "kubernetes-client", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class KubernetesClientBuildConfig {

    /**
     * Whether or not the client should trust a self signed certificate if so presented by the API server
     */
    @ConfigItem
    public Optional<Boolean> trustCerts = Optional.empty();

    /**
     * URL of the Kubernetes API server
     */
    @ConfigItem
    public Optional<String> masterUrl;

    /**
     * Default namespace to use
     */
    @ConfigItem
    public Optional<String> namespace;

    /**
     * CA certificate file
     */
    @ConfigItem
    public Optional<String> caCertFile;

    /**
     * CA certificate data
     */
    @ConfigItem
    public Optional<String> caCertData;

    /**
     * Client certificate file
     */
    @ConfigItem
    public Optional<String> clientCertFile;

    /**
     * Client certificate data
     */
    @ConfigItem
    public Optional<String> clientCertData;

    /**
     * Client key file
     */
    @ConfigItem
    public Optional<String> clientKeyFile;

    /**
     * Client key data
     */
    @ConfigItem
    public Optional<String> clientKeyData;

    /**
     * Client key algorithm
     */
    @ConfigItem
    public Optional<String> clientKeyAlgo;

    /**
     * Client key passphrase
     */
    @ConfigItem
    public Optional<String> clientKeyPassphrase;

    /**
     * Kubernetes auth username
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * Kubernetes auth password
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Kubernetes oauth token
     */
    @ConfigItem
    public Optional<String> token;

    /**
     * Watch reconnect interval
     */
    @ConfigItem(defaultValue = "PT1S") // default lifted from Kubernetes Client
    public Duration watchReconnectInterval;

    /**
     * Maximum reconnect attempts in case of watch failure
     * By default there is no limit to the number of reconnect attempts
     */
    @ConfigItem(defaultValue = "-1") // default lifted from Kubernetes Client
    public int watchReconnectLimit;

    /**
     * Maximum amount of time to wait for a connection with the API server to be established
     */
    @ConfigItem(defaultValue = "PT10S") // default lifted from Kubernetes Client
    public Duration connectionTimeout;

    /**
     * Maximum amount of time to wait for a request to the API server to be completed
     */
    @ConfigItem(defaultValue = "PT10S") // default lifted from Kubernetes Client
    public Duration requestTimeout;

    /**
     * Maximum amount of time in milliseconds to wait for a rollout to be completed
     */
    @ConfigItem(defaultValue = "PT15M") // default lifted from Kubernetes Client
    public Duration rollingTimeout;

    /**
     * HTTP proxy used to access the Kubernetes API server
     */
    @ConfigItem
    public Optional<String> httpProxy;

    /**
     * HTTPS proxy used to access the Kubernetes API server
     */
    @ConfigItem
    public Optional<String> httpsProxy;

    /**
     * Proxy username
     */
    @ConfigItem
    public Optional<String> proxyUsername;

    /**
     * Proxy password
     */
    @ConfigItem
    public Optional<String> proxyPassword;

    /**
     * IP addresses or hosts to exclude from proxying
     */
    @ConfigItem
    public Optional<String[]> noProxy;

}
