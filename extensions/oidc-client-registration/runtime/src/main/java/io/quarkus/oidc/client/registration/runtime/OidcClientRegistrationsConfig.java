package io.quarkus.oidc.client.registration.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.oidc-client-registration")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OidcClientRegistrationsConfig {

    String DEFAULT_CLIENT_REGISTRATION_KEY = "<default>";

    /**
     * Additional named client registrations.
     */
    @ConfigDocMapKey("id")
    @WithParentName
    @WithUnnamedKey(DEFAULT_CLIENT_REGISTRATION_KEY)
    @WithDefaults
    Map<String, io.quarkus.oidc.client.registration.OidcClientRegistrationConfig> namedClientRegistrations();

    static io.quarkus.oidc.client.registration.OidcClientRegistrationConfig getDefaultClientRegistration(
            OidcClientRegistrationsConfig config) {
        return config.namedClientRegistrations().get(DEFAULT_CLIENT_REGISTRATION_KEY);
    }
}
