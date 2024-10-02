package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcResponseFilter;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.REGISTERED_CLIENT)
public class RegisteredClientResponseFilter implements OidcResponseFilter {
    private static final Logger LOG = Logger.getLogger(RegisteredClientResponseFilter.class);

    @Override
    public void filter(OidcResponseContext rc) {
        String contentType = rc.responseHeaders().get("Content-Type");
        if (contentType.startsWith("application/json")
                && "Default Client Updated".equals(rc.responseBody().toJsonObject().getString("client_name"))) {
            LOG.debug("Registered 'Default Client' has had its name updated to 'Default Client Updated'");
        }
    }

}
