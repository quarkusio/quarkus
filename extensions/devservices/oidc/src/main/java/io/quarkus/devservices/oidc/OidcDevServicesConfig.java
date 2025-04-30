package io.quarkus.devservices.oidc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * OpenID Connect Dev Services configuration.
 */
@ConfigRoot
@ConfigMapping(prefix = "quarkus.oidc.devservices")
public interface OidcDevServicesConfig {

    /**
     * Use OpenID Connect Dev Services instead of Keycloak.
     */
    @ConfigDocDefault("Enabled when Docker and Podman are not available")
    Optional<Boolean> enabled();

    /**
     * A map of roles for OIDC identity provider users.
     * <p>
     * If empty, default roles are assigned: user `alice` receives `admin` and `user` roles and user `bob` receives role `user`.
     */
    @ConfigDocMapKey("role-name")
    Map<String, List<String>> roles();

}
