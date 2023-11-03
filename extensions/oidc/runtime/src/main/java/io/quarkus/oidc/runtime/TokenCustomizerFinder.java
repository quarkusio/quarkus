package io.quarkus.oidc.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.TokenCustomizer;

public class TokenCustomizerFinder {

    private TokenCustomizerFinder() {

    }

    public static TokenCustomizer find(OidcTenantConfig oidcConfig) {
        if (oidcConfig == null) {
            return null;
        }
        ArcContainer container = Arc.container();
        if (container != null) {
            String customizerName = oidcConfig.token.customizerName.orElse(null);
            if (customizerName != null && !customizerName.isEmpty()) {
                InstanceHandle<TokenCustomizer> tokenCustomizer = container.instance(customizerName);
                if (tokenCustomizer.isAvailable()) {
                    return tokenCustomizer.get();
                } else {
                    throw new OIDCException("Unable to find TokenCustomizer " + customizerName);
                }
            } else if (oidcConfig.tenantId.isPresent()) {
                return container
                        .instance(TokenCustomizer.class, TenantFeature.TenantFeatureLiteral.of(oidcConfig.tenantId.get()))
                        .get();
            }
        }
        return null;
    }

}
