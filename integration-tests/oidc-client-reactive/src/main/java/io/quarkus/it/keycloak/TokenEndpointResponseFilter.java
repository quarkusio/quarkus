package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.OidcConstants;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.TOKEN)
public class TokenEndpointResponseFilter implements OidcResponseFilter {
    private static final Logger LOG = Logger.getLogger(TokenEndpointResponseFilter.class);

    @Override
    public void filter(OidcResponseContext rc) {
        String contentType = rc.responseHeaders().get("Content-Type");
        if (contentType.equals("application/json")
                && rc.responseBody().toJsonObject().containsKey("refresh_token")
                && "refresh_token".equals(rc.requestProperties().get(OidcConstants.GRANT_TYPE))) {
            LOG.debug("Tokens have been refreshed");
        }
    }

}
