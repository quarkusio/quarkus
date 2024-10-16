package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcResponseFilter;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.USERINFO)
public class SignedUserInfoResponseFilter implements OidcResponseFilter {
    private static final Logger LOG = Logger.getLogger(SignedUserInfoResponseFilter.class);

    @Override
    public void filter(OidcResponseContext rc) {
        String contentType = rc.responseHeaders().get("Content-Type");
        if (contentType.startsWith("application/jwt") && rc.responseBody().toString().startsWith("ey")) {
            LOG.debug("Response contains signed UserInfo");
        }
    }

}
