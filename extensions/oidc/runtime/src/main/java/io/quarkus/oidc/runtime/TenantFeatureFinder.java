package io.quarkus.oidc.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.inject.Default;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.TokenCustomizer;

public class TenantFeatureFinder {

    private TenantFeatureFinder() {

    }

    public static List<TokenCustomizer> find(OidcTenantConfig oidcConfig) {
        if (oidcConfig == null) {
            return List.of();
        }
        ArcContainer container = Arc.container();
        if (container != null) {
            String customizerName = oidcConfig.token.customizerName.orElse(null);
            if (customizerName != null && !customizerName.isEmpty()) {
                InstanceHandle<TokenCustomizer> tokenCustomizer = container.instance(customizerName);
                if (tokenCustomizer.isAvailable()) {
                    return List.of(tokenCustomizer.get());
                } else {
                    throw new OIDCException("Unable to find TokenCustomizer " + customizerName);
                }
            } else {
                return find(oidcConfig, TokenCustomizer.class);
            }
        }
        return List.of();
    }

    public static <T> List<T> find(OidcTenantConfig oidcTenantConfig, Class<T> tenantFeatureClass) {
        if (oidcTenantConfig != null && oidcTenantConfig.tenantId.isPresent()) {
            ArcContainer container = Arc.container();
            if (container != null) {
                var tenantsValidators = new ArrayList<T>();
                for (var instance : container.listAll(tenantFeatureClass, Default.Literal.INSTANCE)) {
                    if (instance.isAvailable()) {
                        tenantsValidators.add(instance.get());
                    }
                }
                tenantsValidators
                        .addAll(findTenantFeaturesByTenantId(tenantFeatureClass, oidcTenantConfig.tenantId.get(), container));
                if (!tenantsValidators.isEmpty()) {
                    return List.copyOf(tenantsValidators);
                }
            }
        }
        return List.of();
    }

    private static <T> List<T> findTenantFeaturesByTenantId(Class<T> tenantFeatureClass, String tenantId,
            ArcContainer container) {
        List<T> list = new ArrayList<>();
        for (T tenantFeature : container.listAll(tenantFeatureClass).stream().map(InstanceHandle::get).toList()) {
            TenantFeature annotation = ClientProxy.unwrap(tenantFeature).getClass().getAnnotation(TenantFeature.class);
            if (annotation != null && Arrays.asList(annotation.value()).contains(tenantId)) {
                list.add(tenantFeature);
            }
        }
        return list;
    }
}
