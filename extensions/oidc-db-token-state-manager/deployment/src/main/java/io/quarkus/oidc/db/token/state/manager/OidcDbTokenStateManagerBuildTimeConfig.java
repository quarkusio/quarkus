package io.quarkus.oidc.db.token.state.manager;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.oidc.db-token-state-manager")
@ConfigRoot
public interface OidcDbTokenStateManagerBuildTimeConfig {

    /**
     * Whether token state should be stored in the database.
     */
    @WithDefault("true")
    boolean enabled();

}
