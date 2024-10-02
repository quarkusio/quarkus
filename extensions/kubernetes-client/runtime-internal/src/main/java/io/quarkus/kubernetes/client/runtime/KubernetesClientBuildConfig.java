package io.quarkus.kubernetes.client.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.kubernetes-client")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface KubernetesClientBuildConfig {

    /**
     * Whether the client should trust a self-signed certificate if so presented by the API server
     */
    Optional<Boolean> trustCerts();

    /**
     * URL of the Kubernetes API server
     */
    Optional<String> apiServerUrl();

    /**
     * Use api-server-url instead.
     */
    @Deprecated(forRemoval = true)
    Optional<String> masterUrl();

    /**
     * Default namespace to use
     */
    Optional<String> namespace();

    /**
     * CA certificate file
     */
    Optional<String> caCertFile();

    /**
     * CA certificate data
     */
    Optional<String> caCertData();

    /**
     * Client certificate file
     */
    Optional<String> clientCertFile();

    /**
     * Client certificate data
     */
    Optional<String> clientCertData();

    /**
     * Client key file
     */
    Optional<String> clientKeyFile();

    /**
     * Client key data
     */
    Optional<String> clientKeyData();

    /**
     * Client key algorithm
     */
    Optional<String> clientKeyAlgo();

    /**
     * Client key passphrase
     */
    Optional<String> clientKeyPassphrase();

    /**
     * Kubernetes auth username
     */
    Optional<String> username();

    /**
     * Kubernetes auth password
     */
    Optional<String> password();

    /**
     * Kubernetes oauth token
     */
    Optional<String> token();

    /**
     * Watch reconnect interval
     */
    Optional<Duration> watchReconnectInterval();

    /**
     * Maximum reconnect attempts in case of watch failure
     * By default there is no limit to the number of reconnect attempts
     */
    OptionalInt watchReconnectLimit();

    /**
     * Maximum amount of time to wait for a connection with the API server to be established
     */
    Optional<Duration> connectionTimeout();

    /**
     * Maximum amount of time to wait for a request to the API server to be completed
     */
    Optional<Duration> requestTimeout();

    /**
     * Maximum number of retry attempts for API requests that fail with an HTTP code of >= 500
     */
    OptionalInt requestRetryBackoffLimit();

    /**
     * Time interval between retry attempts for API requests that fail with an HTTP code of >= 500
     */
    Optional<Duration> requestRetryBackoffInterval();

    /**
     * HTTP proxy used to access the Kubernetes API server
     */
    Optional<String> httpProxy();

    /**
     * HTTPS proxy used to access the Kubernetes API server
     */
    Optional<String> httpsProxy();

    /**
     * Proxy username
     */
    Optional<String> proxyUsername();

    /**
     * Proxy password
     */
    Optional<String> proxyPassword();

    /**
     * IP addresses or hosts to exclude from proxying
     */
    Optional<List<String>> noProxy();

    /**
     * Enable the generation of the RBAC manifests. If enabled and no other role binding are provided using the properties
     * `quarkus.kubernetes.rbac.`, it will generate a default role binding using the role "view" and the application
     * service account.
     */
    @WithDefault("true")
    boolean generateRbac();

    /**
     * Dev Services
     */
    @ConfigDocSection(generated = true)
    KubernetesDevServicesBuildTimeConfig devservices();
}
