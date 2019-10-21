package io.quarkus.infinispan.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * @author Katia Aresti
 */
@ConfigRoot(name = "infinispan-client", phase = ConfigPhase.RUN_TIME)
public class InfinispanClientRuntimeConfig {

    /**
     * Sets the host name/port to connect to. Each one is separated by a semicolon (eg. host1:11222;host2:11222).
     */
    @ConfigItem
    public Optional<String> serverList;

    /**
     * Sets client intelligence used by authentication
     */
    @ConfigItem
    Optional<String> clientIntelligence;

    /**
     * Enables or disables authentication
     */
    @ConfigItem
    Optional<String> useAuth;

    /**
     * Sets user name used by authentication
     */
    @ConfigItem
    Optional<String> authUsername;

    /**
     * Sets password used by authentication
     */
    @ConfigItem
    Optional<String> authPassword;

    /**
     * Sets realm used by authentication
     */
    @ConfigItem
    Optional<String> authRealm;

    /**
     * Sets server name used by authentication
     */
    @ConfigItem
    Optional<String> authServerName;

    /**
     * Sets client subject used by authentication
     */
    @ConfigItem
    Optional<String> authClientSubject;

    /**
     * Sets callback handler used by authentication
     */
    @ConfigItem
    Optional<String> authCallbackHandler;

    /**
     * Sets SASL mechanism used by authentication
     */
    @ConfigItem
    Optional<String> saslMechanism;

    @Override
    public String toString() {
        return "InfinispanClientRuntimeConfig{" +
                "serverList=" + serverList +
                '}';
    }
}
