package io.quarkus.elytron.security.jdbc;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * A configuration object for a jdbc based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.jdbc.JdbcSecurityRealm}
 */
@ConfigMapping(prefix = "quarkus.security.jdbc")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface JdbcSecurityRealmBuildTimeConfig {

    /**
     * The realm name
     */
    @WithDefault("Quarkus")
    String realmName();

    /**
     * If the properties store is enabled.
     */
    @WithDefault("false")
    boolean enabled();

    String toString();
}
