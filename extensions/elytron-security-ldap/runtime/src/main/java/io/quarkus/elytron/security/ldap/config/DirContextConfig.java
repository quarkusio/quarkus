package io.quarkus.elytron.security.ldap.config;

import java.time.Duration;
import java.util.Optional;

import org.wildfly.security.auth.realm.ldap.DirContextFactory;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DirContextConfig {

    /**
     * The url of the ldap server
     */
    String url();

    /**
     * The principal: user which is used to connect to ldap server (also named "bindDn")
     */
    Optional<String> principal();

    /**
     * The password which belongs to the principal (also named "bindCredential")
     */
    Optional<String> password();

    /**
     * how ldap redirects are handled
     */
    @WithDefault("ignore")
    DirContextFactory.ReferralMode referralMode();

    /**
     * The connect timeout
     */
    @WithDefault("5s")
    Duration connectTimeout();

    /**
     * The read timeout
     */
    @WithDefault("60s")
    Duration readTimeout();

    /**
     * The name of the TLS configuration (`quarkus.tls.<name>.*`) to use for the connections to the LDAP server, for
     * example to trust the private certificate authority of an `ldaps://` server.
     * <p>
     * If not set, the default SSL context of the JVM is used.
     */
    Optional<String> tlsConfigurationName();

    String toString();
}
