package io.quarkus.oidc.runtime;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.Default;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.TenantFeature.TenantFeatureLiteral;
import io.quarkus.oidc.TokenCustomizer;

public class TenantFeatureFinder {

    private TenantFeatureFinder() {

    }

    public static TokenCustomizer find(OidcTenantConfig oidcConfig) {
        if (oidcConfig == null) {
            return null;
        }
        ArcContainer container = Arc.container();
        if (container != null) {
            String customizerName = oidcConfig.token().customizerName().orElse(null);
            if (customizerName != null && !customizerName.isEmpty()) {
                InstanceHandle<TokenCustomizer> tokenCustomizer = container.instance(customizerName);
                if (tokenCustomizer.isAvailable()) {
                    return tokenCustomizer.get();
                } else {
                    throw new OIDCException("Unable to find TokenCustomizer " + customizerName);
                }
            } else if (oidcConfig.tenantId().isPresent()) {
                return container
                        .instance(TokenCustomizer.class, TenantFeature.TenantFeatureLiteral.of(oidcConfig.tenantId().get()))
                        .get();
            }
        }
        return null;
    }

    public static <T> List<T> find(OidcTenantConfig oidcTenantConfig, Class<T> tenantFeatureClass) {
        if (oidcTenantConfig != null && oidcTenantConfig.tenantId().isPresent()) {
            var tenantsValidators = new ArrayList<T>();
            for (var instance : Arc.container().listAll(tenantFeatureClass, Default.Literal.INSTANCE)) {
                if (instance.isAvailable()) {
                    tenantsValidators.add(instance.get());
                }
            }
            for (var instance : Arc.container().listAll(tenantFeatureClass,
                    TenantFeatureLiteral.of(oidcTenantConfig.tenantId().get()))) {
                if (instance.isAvailable()) {
                    tenantsValidators.add(instance.get());
                }
            }
            if (!tenantsValidators.isEmpty()) {
                return List.copyOf(tenantsValidators);
            }
        }
        return List.of();
    }
}
