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
     * When set, every URL of `quarkus.security.ldap.dir-context.url` must use the `ldaps://` scheme, and the protocols of
     * the TLS configuration are applied, which default to TLSv1.3 only.
     * <p>
     * If not set, the default SSL context of the JVM is used. The default TLS configuration of the TLS registry is
     * deliberately not used, so that setting it for the HTTP server does not change the LDAP connections.
     */
    Optional<String> tlsConfigurationName();

    String toString();
}
