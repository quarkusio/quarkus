package io.quarkus.devservices.oidc.lightweight;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * OpenID Connect Lightweight Dev Services configuration.
 */
@ConfigRoot
@ConfigMapping(prefix = "quarkus.oidc.devservices.lightweight")
public interface OidcLightweightDevServicesConfig {

    /**
     * Use lightweight Dev Services instead of Keycloak.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * A map of roles for OIDC identity provider users.
     * <p>
     * If empty, default roles are assigned: `alice` receives `admin` and `user` roles, while other users receive
     * `user` role.
     * This map is used for role creation when no realm file is found at the `realm-path`.
     */
    @ConfigDocMapKey("role-name")
    Map<String, List<String>> roles();

}
