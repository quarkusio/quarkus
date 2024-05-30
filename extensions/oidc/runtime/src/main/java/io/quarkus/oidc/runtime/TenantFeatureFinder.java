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
                String tenantId = oidcConfig.tenantId.get();
                List<TokenCustomizer> list = findTenantFeaturesByTenantId(TokenCustomizer.class, tenantId);
                if (!list.isEmpty()) {
                    if (list.size() >= 2) {
                        throw new OIDCException(
                                "Found multiple TokenCustomizers that are annotated with @TenantFeature that has tenantId ("
                                        + tenantId + ")");
                    }
                    return list.get(0);
                }

            }
        }
        return null;
    }

    public static <T> List<T> find(OidcTenantConfig oidcTenantConfig, Class<T> tenantFeatureClass) {
        if (oidcTenantConfig != null && oidcTenantConfig.tenantId.isPresent()) {
            var tenantsValidators = new ArrayList<T>();
            for (var instance : Arc.container().listAll(tenantFeatureClass, Default.Literal.INSTANCE)) {
                if (instance.isAvailable()) {
                    tenantsValidators.add(instance.get());
                }
            }
            tenantsValidators.addAll(findTenantFeaturesByTenantId(tenantFeatureClass, oidcTenantConfig.tenantId.get()));
            if (!tenantsValidators.isEmpty()) {
                return List.copyOf(tenantsValidators);
            }
        }
        return List.of();
    }

    private static <T> List<T> findTenantFeaturesByTenantId(Class<T> tenantFeatureClass, String tenantId) {
        ArcContainer container = Arc.container();
        if (container != null) {
            List<T> list = new ArrayList<>();
            for (T tenantFeature : container.listAll(tenantFeatureClass).stream().map(InstanceHandle::get).toList()) {
                TenantFeature annotation = ClientProxy.unwrap(tenantFeature).getClass().getAnnotation(TenantFeature.class);
                if (annotation != null && Arrays.asList(annotation.value()).contains(tenantId)) {
                    list.add(tenantFeature);
                }
            }
            return list;
        }
        return List.of();
    }
}
