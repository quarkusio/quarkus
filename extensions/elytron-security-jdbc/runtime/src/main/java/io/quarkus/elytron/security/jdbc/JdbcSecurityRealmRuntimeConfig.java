package io.quarkus.elytron.security.jdbc;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

/**
 * A configuration object for a jdbc based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.jdbc.JdbcSecurityRealm}
 */
@ConfigMapping(prefix = "quarkus.security.jdbc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JdbcSecurityRealmRuntimeConfig {

    /**
     * The principal-queries config
     */
    @WithName("principal-query")
    PrincipalQueriesConfig principalQueries();
    //  https://github.com/wildfly/wildfly-core/blob/main/elytron/src/test/resources/org/wildfly/extension/elytron/security-realms.xml#L18

    String toString();
}
