package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.DISCOVERY)
public class OidcDiscoveryRequestCustomizer implements OidcRequestFilter {

    @Override
    public void filter(HttpRequest<Buffer> request, Buffer buffer, OidcRequestContextProperties contextProps) {
        request.putHeader("Discovery", "OK");
    }
}
