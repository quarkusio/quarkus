package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.CLIENT_REGISTRATION)
public class ClientRegistrationResponseFilter implements OidcResponseFilter {
    private static final Logger LOG = Logger.getLogger(ClientRegistrationResponseFilter.class);

    @Override
    public void filter(OidcResponseContext rc) {
        String contentType = rc.responseHeaders().get("Content-Type");
        JsonObject body = rc.responseBody().toJsonObject();
        if (contentType.startsWith("application/json")
                && "Default Client".equals(body.getString("client_name"))) {
            LOG.debug("'Default Client' has been registered");
        }
    }

}
