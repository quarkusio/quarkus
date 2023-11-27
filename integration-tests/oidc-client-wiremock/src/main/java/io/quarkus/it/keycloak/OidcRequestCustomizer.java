package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

@ApplicationScoped
@Unremovable
public class OidcRequestCustomizer implements OidcRequestFilter {

    @Override
    public void filter(HttpRequest<Buffer> request, Buffer buffer, OidcRequestContextProperties contextProps) {
        String uri = request.uri();
        if (uri.endsWith("/non-standard-tokens")) {
            request.putHeader("GrantType", getGrantType(buffer.toString()));
        }
    }

    private String getGrantType(String formString) {
        for (String formValue : formString.split("&")) {
            if (formValue.startsWith("grant_type=")) {
                return formValue.substring("grant_type=".length());
            }
        }
        return "";
    }
}
