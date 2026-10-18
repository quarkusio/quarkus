package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;

@ApplicationScoped
public class DPoPAuthFailureObserver {

    static final String NO_DPOP_ERROR_ATTRIBUTE = "no_dpop_error_attribute";

    private volatile String lastDPoPErrorAttribute;

    void observe(@Observes AuthenticationFailureEvent event) {
        if (event.getAuthenticationFailure() instanceof AuthenticationFailedException ex) {
            if (Boolean.TRUE.equals(ex.getAttribute(OidcConstants.INVALID_DPOP_PROOF))) {
                lastDPoPErrorAttribute = OidcConstants.INVALID_DPOP_PROOF;
            } else if (Boolean.TRUE.equals(ex.getAttribute(OidcConstants.USE_DPOP_NONCE))) {
                lastDPoPErrorAttribute = OidcConstants.USE_DPOP_NONCE;
            } else {
                lastDPoPErrorAttribute = NO_DPOP_ERROR_ATTRIBUTE;
            }
        }
    }

    String getLastDPoPErrorAttribute() {
        return lastDPoPErrorAttribute;
    }

    void reset() {
        lastDPoPErrorAttribute = null;
    }
}
