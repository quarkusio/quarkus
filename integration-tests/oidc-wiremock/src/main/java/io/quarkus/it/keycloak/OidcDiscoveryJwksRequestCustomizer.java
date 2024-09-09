package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = { Type.DISCOVERY, Type.JWKS })
public class OidcDiscoveryJwksRequestCustomizer implements OidcRequestFilter {

    @Override
    public void filter(OidcRequestContext rc) {
        if (!rc.request().uri().endsWith(".well-known/openid-configuration")
                && !isJwksRequest(rc.request())) {
            throw new OIDCException("Filter is applied to the wrong endpoint: " + rc.request().uri());
        }
        rc.request().putHeader("Filter", "OK");
        rc.request().putHeader(OidcUtils.TENANT_ID_ATTRIBUTE, rc.contextProperties().getString(OidcUtils.TENANT_ID_ATTRIBUTE));
    }

    private boolean isJwksRequest(HttpRequest<Buffer> request) {
        return request.uri().endsWith("/protocol/openid-connect/certs")
                || request.uri().endsWith("/auth/azure/jwk")
                || request.uri().endsWith("/single-key-without-kid-thumbprint");
    }
}
