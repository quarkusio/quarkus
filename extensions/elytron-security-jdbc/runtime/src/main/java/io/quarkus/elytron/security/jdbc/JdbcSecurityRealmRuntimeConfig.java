package io.quarkus.elytron.security.jdbc;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * A configuration object for a jdbc based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.jdbc.JdbcSecurityRealm}
 */
@ConfigRoot(name = "security.jdbc", phase = ConfigPhase.RUN_TIME)
public class JdbcSecurityRealmRuntimeConfig {

    /**
     * The principal-queries config
     */
    @ConfigItem(name = "principal-query")
    public PrincipalQueriesConfig principalQueries;
    //  https://github.com/wildfly/wildfly-core/blob/main/elytron/src/test/resources/org/wildfly/extension/elytron/security-realms.xml#L18

    @Override
    public String toString() {
        return "JdbcRealmConfig{" +
                "principalQueries=" + principalQueries +
                '}';
    }
}
