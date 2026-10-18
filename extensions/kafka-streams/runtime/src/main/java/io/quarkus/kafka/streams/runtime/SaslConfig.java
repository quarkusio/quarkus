package io.quarkus.kafka.streams.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface SaslConfig {

    /**
     * SASL mechanism used for client connections
     */
    Optional<String> mechanism();

    /**
     * JAAS login context parameters for SASL connections in the format used by JAAS configuration files
     */
    Optional<String> jaasConfig();

    /**
     * The fully qualified name of a SASL client callback handler class
     */
    Optional<String> clientCallbackHandlerClass();

    /**
     * The fully qualified name of a SASL login callback handler class
     */
    Optional<String> loginCallbackHandlerClass();

    /**
     * The fully qualified name of a class that implements the Login interface
     */
    Optional<String> loginClass();

    /**
     * The Kerberos principal name that Kafka runs as
     */
    Optional<String> kerberosServiceName();

    /**
     * Kerberos kinit command path
     */
    Optional<String> kerberosKinitCmd();

    /**
     * Login thread will sleep until the specified window factor of time from last refresh
     */
    Optional<Double> kerberosTicketRenewWindowFactor();

    /**
     * Percentage of random jitter added to the renewal time
     */
    Optional<Double> kerberosTicketRenewJitter();

    /**
     * Percentage of random jitter added to the renewal time
     */
    Optional<Long> kerberosMinTimeBeforeRelogin();

    /**
     * Login refresh thread will sleep until the specified window factor relative to the
     * credential's lifetime has been reached-
     */
    Optional<Double> loginRefreshWindowFactor();

    /**
     * The maximum amount of random jitter relative to the credential's lifetime
     */
    Optional<Double> loginRefreshWindowJitter();

    /**
     * The desired minimum duration for the login refresh thread to wait before refreshing a credential
     */
    Optional<Duration> loginRefreshMinPeriod();

    /**
     * The amount of buffer duration before credential expiration to maintain when refreshing a credential
     */
    Optional<Duration> loginRefreshBuffer();

}
