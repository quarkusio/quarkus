package io.quarkus.kafka.streams.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SaslConfig {

    /**
     * SASL mechanism used for client connections
     */
    @ConfigItem
    public Optional<String> mechanism;

    /**
     * JAAS login context parameters for SASL connections in the format used by JAAS configuration files
     */
    @ConfigItem
    public Optional<String> jaasConfig;

    /**
     * The fully qualified name of a SASL client callback handler class
     */
    @ConfigItem
    public Optional<String> clientCallbackHandlerClass;

    /**
     * The fully qualified name of a SASL login callback handler class
     */
    @ConfigItem
    public Optional<String> loginCallbackHandlerClass;

    /**
     * The fully qualified name of a class that implements the Login interface
     */
    @ConfigItem
    public Optional<String> loginClass;

    /**
     * The Kerberos principal name that Kafka runs as
     */
    @ConfigItem
    public Optional<String> kerberosServiceName;

    /**
     * Kerberos kinit command path
     */
    @ConfigItem
    public Optional<String> kerberosKinitCmd;

    /**
     * Login thread will sleep until the specified window factor of time from last refresh
     */
    @ConfigItem
    public Optional<Double> kerberosTicketRenewWindowFactor;

    /**
     * Percentage of random jitter added to the renewal time
     */
    @ConfigItem
    public Optional<Double> kerberosTicketRenewJitter;

    /**
     * Percentage of random jitter added to the renewal time
     */
    @ConfigItem
    public Optional<Long> kerberosMinTimeBeforeRelogin;

    /**
     * Login refresh thread will sleep until the specified window factor relative to the
     * credential's lifetime has been reached-
     */
    @ConfigItem
    public Optional<Double> loginRefreshWindowFactor;

    /**
     * The maximum amount of random jitter relative to the credential's lifetime
     */
    @ConfigItem
    public Optional<Double> loginRefreshWindowJitter;

    /**
     * The desired minimum duration for the login refresh thread to wait before refreshing a credential
     */
    @ConfigItem
    public Optional<Duration> loginRefreshMinPeriod;

    /**
     * The amount of buffer duration before credential expiration to maintain when refreshing a credential
     */
    @ConfigItem
    public Optional<Duration> loginRefreshBuffer;

}
