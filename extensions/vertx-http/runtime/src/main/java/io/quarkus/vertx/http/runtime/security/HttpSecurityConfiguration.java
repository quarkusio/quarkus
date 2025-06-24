package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.event.Event;

import io.quarkus.arc.Arc;
import io.quarkus.vertx.http.runtime.AuthRuntimeConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.security.HttpSecurity;

/**
 * This singleton carries final HTTP Security configuration and act as a single source of truth for it.
 */
record HttpSecurityConfiguration(RolesMapping rolesMapping, List<HttpPermissionCarrier> httpPermissions) {

    private static volatile HttpSecurityConfiguration instance = null;

    record Policy(String name, HttpSecurityPolicy instance) {
    }

    record AuthenticationMechanism(String name, HttpAuthenticationMechanism instance) {
    }

    interface HttpPermissionCarrier {

        Set<String> getPaths();

        boolean isShared();

        boolean shouldApplyToJaxRs();

        Set<String> getMethods();

        AuthenticationMechanism getAuthMechanism();

        Policy getPolicy();

        default PolicyMappingConfig.AppliesTo getAppliesTo() {
            return shouldApplyToJaxRs() ? PolicyMappingConfig.AppliesTo.JAXRS : PolicyMappingConfig.AppliesTo.ALL;
        }
    }

    // this instance is not in the CDI container to avoid "potential" (I am guessing) circular dependencies
    // during the bean instantiation as we can't be sure what users will inject when they observe the HTTP Security;
    // we could get 'VertxHttpConfig' from SR Config, but this way, we have "guaranteed" that the runtime config is ready
    static HttpSecurityConfiguration get(VertxHttpConfig vertxHttpConfig) {
        if (instance == null) {
            synchronized (HttpSecurityConfiguration.class) {
                if (instance == null) {
                    HttpSecurityImpl httpSecurity = prepareHttpSecurity(vertxHttpConfig.auth());
                    instance = new HttpSecurityConfiguration(httpSecurity.getRolesMapping(), httpSecurity.getHttpPermissions());
                }
            }
        }
        return instance;
    }

    private static HttpSecurityImpl prepareHttpSecurity(AuthRuntimeConfig authConfig) {
        HttpSecurityImpl httpSecurity = new HttpSecurityImpl();
        addAuthRuntimeConfigToHttpSecurity(authConfig, httpSecurity);
        Event<HttpSecurity> httpSecurityEvent = Arc.container().beanManager().getEvent().select(HttpSecurity.class);
        httpSecurityEvent.fire(httpSecurity);
        return httpSecurity;
    }

    private static void addAuthRuntimeConfigToHttpSecurity(AuthRuntimeConfig authConfig, HttpSecurityImpl httpSecurity) {
        if (!authConfig.rolesMapping().isEmpty()) {
            httpSecurity.rolesMapping(authConfig.rolesMapping());
        }
        List<HttpPermissionCarrier> httpPermissions = adaptToHttpPermissionCarriers(authConfig.permissions());
        httpSecurity.addHttpPermissions(httpPermissions);
    }

    static List<HttpPermissionCarrier> adaptToHttpPermissionCarriers(Map<String, PolicyMappingConfig> mappings) {
        List<HttpPermissionCarrier> httpPermissions = new ArrayList<>();
        for (PolicyMappingConfig mappingConfig : mappings.values()) {
            HttpPermissionCarrier httpPermissionCarrier = adaptToHttpPermissionCarrier(mappingConfig);
            if (httpPermissionCarrier != null) {
                httpPermissions.add(httpPermissionCarrier);
            }
        }
        return httpPermissions;
    }

    private static HttpPermissionCarrier adaptToHttpPermissionCarrier(PolicyMappingConfig mapping) {
        if (!mapping.enabled().orElse(true)) {
            // permission disabled
            return null;
        }
        if (mapping.paths().isEmpty() || mapping.paths().get().isEmpty()) {
            // no paths means no path-based HTTP permission
            return null;
        }
        return new HttpPermissionCarrier() {
            @Override
            public Set<String> getPaths() {
                return Set.copyOf(mapping.paths().get());
            }

            @Override
            public boolean isShared() {
                return mapping.shared();
            }

            @Override
            public boolean shouldApplyToJaxRs() {
                return mapping.appliesTo() == PolicyMappingConfig.AppliesTo.JAXRS;
            }

            @Override
            public Set<String> getMethods() {
                if (mapping.methods().isEmpty()) {
                    return Set.of();
                }
                return Set.copyOf(mapping.methods().get());
            }

            @Override
            public AuthenticationMechanism getAuthMechanism() {
                if (mapping.authMechanism().isPresent()) {
                    String authMech = mapping.authMechanism().get();
                    if (!authMech.isEmpty()) {
                        return new AuthenticationMechanism(authMech, null);
                    }
                }
                return null;
            }

            @Override
            public Policy getPolicy() {
                return new Policy(mapping.policy(), null);
            }

            @Override
            public PolicyMappingConfig.AppliesTo getAppliesTo() {
                return mapping.appliesTo();
            }
        };
    }
}
