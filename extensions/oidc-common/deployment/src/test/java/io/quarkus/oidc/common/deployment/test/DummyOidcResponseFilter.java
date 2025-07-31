package io.quarkus.oidc.common.deployment.test;

import io.quarkus.oidc.common.OidcResponseFilter;

public class DummyOidcResponseFilter implements OidcResponseFilter {
    @Override
    public void filter(OidcResponseContext responseContext) {
    }
}
