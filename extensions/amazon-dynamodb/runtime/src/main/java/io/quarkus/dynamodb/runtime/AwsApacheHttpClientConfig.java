package io.quarkus.dynamodb.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AwsApacheHttpClientConfig {

    /**
     * Connection acquisition timeout
     */
    @ConfigItem
    public Optional<Duration> connectionAcquisitionTimeout;

    /**
     * Connection max idle time
     */
    @ConfigItem
    public Optional<Duration> connectionMaxIdleTime;

    /**
     * Connection timeout
     */
    @ConfigItem
    public Optional<Duration> connectionTimeout;

    /**
     * Connection time to live
     */
    @ConfigItem
    public Optional<Duration> connectionTimeToLive;

    /**
     * Socket timeout
     */
    @ConfigItem
    public Optional<Duration> socketTimeout;

    /**
     * Max connections
     */
    @ConfigItem
    public OptionalInt maxConnections;

    /**
     * Expect continue enabled
     */
    @ConfigItem
    public Optional<Boolean> expectContinueEnabled;

    /**
     * Use idle connection reaper
     */
    @ConfigItem
    public Optional<Boolean> useIdleConnectionReaper;

    /**
     * HTTP proxy configuration
     */
    @ConfigItem
    public HttpClientProxyConfiguration proxy;

    @ConfigGroup
    public static class HttpClientProxyConfiguration {

        /**
         * Enable HTTP proxy
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;

        /**
         * Proxy endpoint
         */
        @ConfigItem
        public URI endpoint;

        /**
         * Proxy username
         */
        @ConfigItem
        public Optional<String> username;

        /**
         * Proxy password
         */
        @ConfigItem
        public Optional<String> password;

        /**
         * NTLM domain
         */
        @ConfigItem
        public Optional<String> ntlmDomain;

        /**
         * NTLM workstation
         */
        @ConfigItem
        public Optional<String> ntlmWorkstation;

        /**
         * Enable preemptive basic authentication
         */
        @ConfigItem
        public Optional<Boolean> preemptiveBasicAuthenticationEnabled;

        /**
         * Configure the hosts that the client is allowed to access without going through the proxy.
         */
        @ConfigItem
        public List<String> nonProxyHosts;
    }
}
