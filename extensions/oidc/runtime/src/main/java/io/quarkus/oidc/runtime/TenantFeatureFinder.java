package io.quarkus.oidc.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Named;
import jakarta.json.JsonObject;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
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
            String customizerName = oidcConfig.token().customizerName().orElse(null);
            if (customizerName != null && !customizerName.isEmpty()) {
                InstanceHandle<TokenCustomizer> tokenCustomizer = container.instance(customizerName);
                if (tokenCustomizer.isAvailable()) {
                    return tokenCustomizer.get();
                } else {
                    throw new OIDCException("Unable to find TokenCustomizer " + customizerName);
                }
            } else if (oidcConfig.tenantId().isPresent()) {
                List<TokenCustomizer> tokenCustomizers = find(oidcConfig, TokenCustomizer.class);
                if (tokenCustomizers.isEmpty()) {
                    return null;
                }
                if (tokenCustomizers.size() == 1) {
                    return tokenCustomizers.get(0);
                }
                return new TokenCustomizer() {
                    @Override
                    public JsonObject customizeHeaders(JsonObject headers) {
                        JsonObject result = headers;
                        for (TokenCustomizer tokenCustomizer : tokenCustomizers) {
                            var customizedHeaders = tokenCustomizer.customizeHeaders(result);
                            if (customizedHeaders != null) {
                                result = customizedHeaders;
                            }
                        }
                        return result == headers ? null : result;
                    }
                };
            }
        }
        return null;
    }

    public static <T> List<T> find(OidcTenantConfig oidcTenantConfig, Class<T> tenantFeatureClass) {
        ArcContainer container = Arc.container();
        if (oidcTenantConfig != null && container != null) {
            var tenantsValidators = new ArrayList<T>();
            allFeatureClasses: for (InstanceHandle<T> instance : container.listAll(tenantFeatureClass)) {
                if (instance.isAvailable()) {
                    qualifiers: for (Annotation qualifier : instance.getBean().getQualifiers()) {
                        if (qualifier instanceof TenantFeature tenantFeature) {
                            String thisTenantId = oidcTenantConfig.tenantId().get();
                            for (String thatTenantId : tenantFeature.value()) {
                                if (thisTenantId.equals(thatTenantId)) {
                                    // adds tenant validator
                                    break qualifiers;
                                }
                            }
                            // don't continue as this is a TenantFeature but not for our tenant
                            continue allFeatureClasses;
                        } else if (qualifier instanceof Named) {
                            // following is done so that we don't include some features that are not meant to be global
                            // but users want to include them using configuration properties
                            // like 'io.quarkus.oidc.runtime.providers.AzureAccessTokenCustomizer'
                            continue allFeatureClasses;
                        }
                    }
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
