package io.quarkus.oidc.client.registration.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.oidc-client-registration")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OidcClientRegistrationsConfig {

    /**
     * The default client registration.
     */
    @WithParentName
    OidcClientRegistrationConfig defaultClientRegistration();

    /**
     * Additional named client registrations.
     */
    @ConfigDocSection
    @ConfigDocMapKey("id")
    @WithParentName
    Map<String, OidcClientRegistrationConfig> namedClientRegistrations();
}
