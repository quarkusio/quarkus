package io.quarkus.oidc.common.deployment.test;

import io.quarkus.oidc.common.OidcRequestFilter;

public class DummyOidcRequestFilter implements OidcRequestFilter {
    @Override
    public void filter(OidcRequestContext requestContext) {
    }
}
