package io.quarkus.oidc.db.token.state.manager.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.oidc.db-token-state-manager")
@ConfigRoot(phase = RUN_TIME)
public interface OidcDbTokenStateManagerRunTimeConfig {

    /**
     * How often should Quarkus check for expired tokens.
     */
    @WithDefault("8h")
    Duration deleteExpiredDelay();

    /**
     * Whether Quarkus should attempt to create database table where the token state is going to be stored.
     */
    @WithDefault("true")
    boolean createDatabaseTableIfNotExists();
}
