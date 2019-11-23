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
    @ConfigItem(defaultValue = "false")
    boolean trustCerts;

    /**
     * URL of the Kubernetes API server
     */
    @ConfigItem
    Optional<String> masterUrl;

    /**
     * Default namespace to use
     */
    @ConfigItem
    Optional<String> namespace;

    /**
     * CA certificate file
     */
    @ConfigItem
    Optional<String> caCertFile;

    /**
     * CA certificate data
     */
    @ConfigItem
    Optional<String> caCertData;

    /**
     * Client certificate file
     */
    @ConfigItem
    Optional<String> clientCertFile;

    /**
     * Client certificate data
     */
    @ConfigItem
    Optional<String> clientCertData;

    /**
     * Client key file
     */
    @ConfigItem
    Optional<String> clientKeyFile;

    /**
     * Client key data
     */
    @ConfigItem
    Optional<String> clientKeyData;

    /**
     * Client key algorithm
     */
    @ConfigItem
    Optional<String> clientKeyAlgo;

    /**
     * Client key passphrase
     */
    @ConfigItem
    Optional<String> clientKeyPassphrase;

    /**
     * Kubernetes auth username
     */
    @ConfigItem
    Optional<String> username;

    /**
     * Kubernetes auth password
     */
    @ConfigItem
    Optional<String> password;

    /**
     * Watch reconnect interval
     */
    @ConfigItem(defaultValue = "PT1S") // default lifted from Kubernetes Client
    Duration watchReconnectInterval;

    /**
     * Maximum reconnect attempts in case of watch failure
     * By default there is no limit to the number of reconnect attempts
     */
    @ConfigItem(defaultValue = "-1") // default lifted from Kubernetes Client
    int watchReconnectLimit;

    /**
     * Maximum amount of time to wait for a connection with the API server to be established
     */
    @ConfigItem(defaultValue = "PT10S") // default lifted from Kubernetes Client
    Duration connectionTimeout;

    /**
     * Maximum amount of time to wait for a request to the API server to be completed
     */
    @ConfigItem(defaultValue = "PT10S") // default lifted from Kubernetes Client
    Duration requestTimeout;

    /**
     * Maximum amount of time in milliseconds to wait for a rollout to be completed
     */
    @ConfigItem(defaultValue = "PT15M") // default lifted from Kubernetes Client
    Duration rollingTimeout;

    /**
     * HTTP proxy used to access the Kubernetes API server
     */
    @ConfigItem
    Optional<String> httpProxy;

    /**
     * HTTPS proxy used to access the Kubernetes API server
     */
    @ConfigItem
    Optional<String> httpsProxy;

    /**
     * Proxy username
     */
    @ConfigItem
    Optional<String> proxyUsername;

    /**
     * Proxy password
     */
    @ConfigItem
    Optional<String> proxyPassword;

    /**
     * IP addresses or hosts to exclude from proxying
     */
    @ConfigItem
    Optional<String[]> noProxy;
}
