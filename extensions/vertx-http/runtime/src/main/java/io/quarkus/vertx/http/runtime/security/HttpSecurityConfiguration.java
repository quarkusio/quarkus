package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.event.Event;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.Arc;
import io.quarkus.vertx.http.runtime.AuthRuntimeConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.security.event.Basic;
import io.quarkus.vertx.http.security.event.Form;
import io.quarkus.vertx.http.security.event.HttpSecurity;
import io.smallrye.config.SmallRyeConfig;

/**
 * This singleton carries final HTTP Security configuration and act as a single source of truth for it.
 */
record HttpSecurityConfiguration(RolesMapping rolesMapping, List<HttpPermissionCarrier> httpPermissions, BasicImpl basicAuth,
        FormImpl formAuth) {

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
    // during the bean instantiation as we can't be sure what users will inject when they observe the HTTP Security
    static HttpSecurityConfiguration get() {
        if (instance == null) {
            synchronized (HttpSecurityConfiguration.class) {
                if (instance == null) {
                    VertxHttpConfig vertxHttpConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
                            .getConfigMapping(VertxHttpConfig.class);
                    HttpSecurityImpl httpSecurity = prepareHttpSecurity(vertxHttpConfig.auth());
                    BasicImpl basicAuth = prepareBasicAuthentication(vertxHttpConfig.auth());
                    FormImpl formAuth = prepareFormBasedAuthentication(vertxHttpConfig);
                    instance = new HttpSecurityConfiguration(httpSecurity.getRolesMapping(), httpSecurity.getHttpPermissions(),
                            basicAuth, formAuth);
                }
            }
        }
        return instance;
    }

    private static BasicImpl prepareBasicAuthentication(AuthRuntimeConfig auth) {
        Optional<Boolean> basicEnabled = ConfigProvider.getConfig().getOptionalValue("quarkus.http.auth.basic", Boolean.class);
        String authenticationRealm = auth.realm().orElse(null);
        BasicImpl basic = new BasicImpl(basicEnabled, authenticationRealm);
        Event<Basic> basicEvent = Arc.container().beanManager().getEvent().select(Basic.class);
        basicEvent.fire(basic);
        return basic.build();
    }

    private static FormImpl prepareFormBasedAuthentication(VertxHttpConfig httpConfig) {
        boolean formAuthEnabled = ConfigProvider.getConfig().getValue("quarkus.http.auth.form.enabled", Boolean.class);
        FormImpl form = new FormImpl(formAuthEnabled, httpConfig.auth().form(), httpConfig.encryptionKey());
        Event<Form> formEvent = Arc.container().beanManager().getEvent().select(Form.class);
        formEvent.fire(form);
        return form.build();
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
