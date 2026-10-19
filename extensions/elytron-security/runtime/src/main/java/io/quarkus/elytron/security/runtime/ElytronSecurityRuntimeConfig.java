package io.quarkus.elytron.security.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Elytron security configuration.
 */
@ConfigMapping(prefix = "quarkus.security.elytron")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ElytronSecurityRuntimeConfig {

    /**
     * The name of the default realm of the Elytron security domain, which is the realm that authenticates users.
     * <p>
     * This property is only used when multiple realms are enabled, for example, the properties file realm and the JDBC
     * realm. Its value must be the realm name of one of the enabled realms. When exactly one realm is enabled, that realm
     * is always the default realm.
     */
    @WithDefault("Quarkus")
    String defaultRealmName();

}
