package io.quarkus.elytron.security.jdbc;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * A configuration object for a jdbc based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.jdbc.JdbcSecurityRealm}
 */
@ConfigRoot(name = "security.jdbc", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JdbcSecurityRealmBuildTimeConfig {

    /**
     * The realm name
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realmName;

    /**
     * If the properties store is enabled.
     */
    @ConfigItem
    public boolean enabled;

    @Override
    public String toString() {
        return "JdbcRealmConfig{" +
                ", realmName='" + realmName + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
