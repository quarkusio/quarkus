package io.quarkus.oidc.client.registration.runtime;

import java.util.function.Supplier;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.registration.OidcClientRegistrations;

/**
 * The default {@code OidcClientRegistration} bean is only active when the default OIDC client registration
 * is configured.
 */
public final class DefaultOidcClientRegistrationActiveCheck implements Supplier<ActiveResult> {

    @Override
    public ActiveResult get() {
        OidcClientRegistrations clientRegistrations = Arc.requireContainer().instance(OidcClientRegistrations.class).get();
        if (clientRegistrations.getClientRegistration() == null) {
            return ActiveResult.inactive(
                    """
                            Default OIDC client registration is not configured. \
                            To activate it, set either the 'quarkus.oidc-client-registration.auth-server-url' \
                            or the absolute 'quarkus.oidc-client-registration.registration-path' URL. \
                            Named and dynamically created OIDC client registrations are available from the 'OidcClientRegistrations' bean. \
                            Refer to https://quarkus.io/guides/security-openid-connect-client-registration for guidance.""");
        }
        return ActiveResult.active();
    }
}
