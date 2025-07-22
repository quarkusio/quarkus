package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.CLIENT_REGISTRATION)
public class ClientRegistrationRequestFilter implements OidcRequestFilter {
    private static final Logger LOG = Logger.getLogger(ClientRegistrationRequestFilter.class);

    @Override
    public void filter(OidcRequestContext rc) {
        JsonObject body = rc.requestBody().toJsonObject();
        if ("Default Client".equals(body.getString("client_name"))) {
            LOG.debug("'Default Client' registration request");
        } else if ("Dynamic Tenant Client".equals(body.getString("client_name"))) {
            body.put("client_name", "Registered Dynamic Tenant Client");
            rc.requestBody(Buffer.buffer(body.toString()));
        }
    }

}
