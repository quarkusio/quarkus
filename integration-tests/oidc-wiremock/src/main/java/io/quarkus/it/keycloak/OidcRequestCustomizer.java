package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

@ApplicationScoped
@Unremovable
public class OidcRequestCustomizer implements OidcRequestFilter {

    @Override
    public void filter(HttpRequest<Buffer> request, Buffer buffer, OidcRequestContextProperties contextProps) {
        HttpMethod method = request.method();
        String uri = request.uri();
        if (method == HttpMethod.GET && uri.endsWith("/auth/azure/jwk")) {
            request.putHeader("Authorization", "ID token");
        }
    }

}
