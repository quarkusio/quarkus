package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        String path = context.normalizedPath();
        if (path.contains("recovered-no-discovery")) {
            return "no-discovery";
        }
        if (path.endsWith("code-flow") || path.endsWith("code-flow/logout")) {
            return "code-flow";
        }
        if (path.endsWith("code-flow-encrypted-id-token-jwk")) {
            return "code-flow-encrypted-id-token-jwk";
        }
        if (path.endsWith("code-flow-encrypted-id-token-pem")) {
            return "code-flow-encrypted-id-token-pem";
        }
        if (path.endsWith("code-flow-form-post") || path.endsWith("code-flow-form-post/front-channel-logout")) {
            return "code-flow-form-post";
        }
        if (path.endsWith("code-flow-user-info-only")) {
            return "code-flow-user-info-only";
        }
        if (path.endsWith("code-flow-user-info-github")) {
            return "code-flow-user-info-github";
        }
        if (path.endsWith("bearer-user-info-github-service")) {
            return "bearer-user-info-github-service";
        }
        if (path.endsWith("code-flow-user-info-github-cached-in-idtoken")) {
            return "code-flow-user-info-github-cached-in-idtoken";
        }
        if (path.endsWith("code-flow-token-introspection")) {
            return "code-flow-token-introspection";
        }
        if (path.endsWith("bearer")) {
            return "bearer";
        }
        if (path.endsWith("bearer-required-algorithm")) {
            return "bearer-required-algorithm";
        }
        if (path.endsWith("bearer-azure")) {
            return "bearer-azure";
        }
        if (path.endsWith("bearer-no-introspection")) {
            return "bearer-no-introspection";
        }
        if (path.endsWith("bearer-role-claim-path")) {
            return "bearer-role-claim-path";
        }
        if (path.endsWith("bearer-key-without-kid-thumbprint")) {
            return "bearer-key-without-kid-thumbprint";
        }
        if (path.endsWith("bearer-wrong-role-path")) {
            return "bearer-wrong-role-path";
        }
        return null;
    }
}
