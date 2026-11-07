package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.TokenVerificationResult;

/**
 * Fails the back-channel logout of the {@link #SUBJECT} subject only.
 */
@ApplicationScoped
public class ThrowingBackChannelLogoutObserver {

    public static final String SUBJECT = "throwing-observer";

    void event(@Observes SecurityEvent event) {
        if (SecurityEvent.Type.OIDC_BACKCHANNEL_LOGOUT_INITIATED == event.getEventType()) {
            TokenVerificationResult result = (TokenVerificationResult) event.getEventProperties()
                    .get(OidcConstants.BACK_CHANNEL_LOGOUT_TOKEN);
            if (SUBJECT.equals(result.localVerificationResult().getString("sub"))) {
                throw new IllegalStateException("boom");
            }
        }
    }
}
