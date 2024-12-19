package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OidcProviderClient;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.oidc.SecurityEvent;

@ApplicationScoped
public class SecurityEventListener {

    public void event(@ObservesAsync SecurityEvent event) {
        if (SecurityEvent.Type.OIDC_BACKCHANNEL_LOGOUT_COMPLETED == event.getEventType()) {
            OidcProviderClient oidcProvider = event.getSecurityIdentity().getAttribute(OidcProviderClient.class.getName());
            String accessToken = event.getSecurityIdentity().getCredential(AccessTokenCredential.class).getToken();
            String refreshToken = event.getSecurityIdentity().getCredential(RefreshToken.class).getToken();
            oidcProvider.revokeAccessToken(accessToken).await().indefinitely();
            oidcProvider.revokeRefreshToken(refreshToken).await().indefinitely();
        }
    }

}
