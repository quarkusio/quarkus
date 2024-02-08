package io.quarkus.oidc.client.registration.runtime;

import java.util.Map;

import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "oidc-client-registration", phase = ConfigPhase.RUN_TIME)
public class OidcClientRegistrationsConfig {

    /**
     * The default client registration.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public OidcClientRegistrationConfig defaultClientRegistration;

    /**
     * Additional named client registrations.
     */
    @ConfigDocSection
    @ConfigDocMapKey("id")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, OidcClientRegistrationConfig> namedClientRegistrations;
}
