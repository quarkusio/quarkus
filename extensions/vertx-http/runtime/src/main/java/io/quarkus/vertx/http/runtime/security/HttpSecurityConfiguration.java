package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.security.HttpAuthenticator.BASIC_AUTH_ANNOTATION_DETECTED;
import static io.quarkus.vertx.http.runtime.security.HttpAuthenticator.TEST_IF_BASIC_AUTH_IMPLICITLY_REQUIRED;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.vertx.http.runtime.AuthRuntimeConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.security.HttpSecurity;
import io.smallrye.config.SmallRyeConfig;

/**
 * This singleton carries final HTTP Security configuration and act as a single source of truth for it.
 */
record HttpSecurityConfiguration(RolesMapping rolesMapping, List<HttpPermissionCarrier> httpPermissions,
        Optional<Boolean> basicAuthEnabled, boolean formAuthEnabled, String formPostLocation,
        List<HttpAuthenticationMechanism> additionalMechanisms) {

    private static final Logger LOG = Logger.getLogger(HttpSecurityConfiguration.class);

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

    BasicAuthenticationMechanism getBasicAuthenticationMechanism(VertxHttpConfig httpConfig) {
        for (HttpAuthenticationMechanism additionalMechanism : additionalMechanisms) {
            if (additionalMechanism.getClass() == BasicAuthenticationMechanism.class) {
                return (BasicAuthenticationMechanism) additionalMechanism;
            }
        }
        return new BasicAuthenticationMechanism(httpConfig.auth().realm().orElse(null), formAuthEnabled);
    }

    FormAuthenticationMechanism getFormAuthenticationMechanism(VertxHttpConfig httpConfig) {
        for (HttpAuthenticationMechanism additionalMechanism : additionalMechanisms) {
            if (additionalMechanism.getClass() == FormAuthenticationMechanism.class) {
                return (FormAuthenticationMechanism) additionalMechanism;
            }
        }
        return new FormAuthenticationMechanism(httpConfig.auth().form(), httpConfig.encryptionKey());
    }

    HttpAuthenticationMechanism[] getMechanisms(Instance<IdentityProvider<?>> providers, boolean inclusiveAuth) {
        Instance<HttpAuthenticationMechanism> mechanismsFromCdi = Arc.container().select(HttpAuthenticationMechanism.class);
        final HttpAuthenticationMechanism[] result;
        List<HttpAuthenticationMechanism> mechanisms = new ArrayList<>();
        for (HttpAuthenticationMechanism mechanism : mechanismsFromCdi) {
            addAuthenticationMechanism(providers, mechanism, mechanisms);
        }
        for (HttpAuthenticationMechanism mechanism : additionalMechanisms) {
            addAuthenticationMechanism(providers, mechanism, mechanisms);
        }
        addBasicAuthMechanismIfImplicitlyRequired(mechanismsFromCdi, mechanisms, providers);
        if (mechanisms.isEmpty()) {
            result = new HttpAuthenticationMechanism[] { new HttpAuthenticator.NoAuthenticationMechanism() };
        } else {
            mechanisms.sort(new Comparator<HttpAuthenticationMechanism>() {
                @Override
                public int compare(HttpAuthenticationMechanism mech1, HttpAuthenticationMechanism mech2) {
                    //descending order
                    return Integer.compare(mech2.getPriority(), mech1.getPriority());
                }
            });
            result = mechanisms.toArray(new HttpAuthenticationMechanism[mechanisms.size()]);

            // if inclusive auth and mTLS are enabled, the mTLS must have the highest priority
            if (inclusiveAuth && mechanismsFromCdi.select(MtlsAuthenticationMechanism.class).isResolvable()) {
                var topMechanism = ClientProxy.unwrap(result[0]);
                boolean isMutualTls = topMechanism instanceof MtlsAuthenticationMechanism;
                if (!isMutualTls) {
                    throw new IllegalStateException(
                            """
                                    Inclusive authentication is enabled and '%s' does not have
                                    the highest priority. Please lower priority of the '%s' authentication mechanism under '%s'.
                                    """.formatted(MtlsAuthenticationMechanism.class.getName(),
                                    topMechanism.getClass().getName(),
                                    MtlsAuthenticationMechanism.INCLUSIVE_AUTHENTICATION_PRIORITY));
                }
            }
        }
        return result;
    }

    // this instance is not in the CDI container to avoid "potential" (I am guessing) circular dependencies
    // during the bean instantiation as we can't be sure what users will inject when they observe the HTTP Security
    static HttpSecurityConfiguration get() {
        if (instance == null) {
            synchronized (HttpSecurityConfiguration.class) {
                if (instance == null) {
                    SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
                    VertxHttpConfig vertxHttpConfig = config.getConfigMapping(VertxHttpConfig.class);
                    HttpSecurityImpl httpSecurity = prepareHttpSecurity(vertxHttpConfig.auth());
                    List<HttpAuthenticationMechanism> mechanisms = httpSecurity.getMechanisms();

                    Optional<Boolean> basicAuthEnabled = config.getOptionalValue("quarkus.http.auth.basic", boolean.class);
                    if (basicAuthEnabled.isEmpty() || !basicAuthEnabled.get()) {
                        for (HttpAuthenticationMechanism mechanism : mechanisms) {
                            // not using instance of as we are not considering subclasses
                            if (mechanism.getClass() == BasicAuthenticationMechanism.class) {
                                basicAuthEnabled = Optional.of(Boolean.TRUE);
                                break;
                            }
                        }
                    }

                    boolean formAuthEnabled = config.getValue("quarkus.http.auth.form.enabled", Boolean.class);
                    String formPostLocation = vertxHttpConfig.auth().form().postLocation();
                    if (!formAuthEnabled) {
                        for (HttpAuthenticationMechanism mechanism : mechanisms) {
                            // not using instance of as we are not considering subclasses
                            if (mechanism.getClass() == FormAuthenticationMechanism.class) {
                                formAuthEnabled = true;
                                formPostLocation = ((FormAuthenticationMechanism) mechanism).getPostLocation();
                                break;
                            }
                        }
                    }

                    instance = new HttpSecurityConfiguration(httpSecurity.getRolesMapping(), httpSecurity.getHttpPermissions(),
                            basicAuthEnabled, formAuthEnabled, formPostLocation, mechanisms);
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

    private void addAuthenticationMechanism(Instance<IdentityProvider<?>> providers,
            HttpAuthenticationMechanism mechanism, List<HttpAuthenticationMechanism> mechanisms) {
        if (mechanism.getCredentialTypes().isEmpty()) {
            // mechanism does not require any IdentityProvider
            LOG.debugf("HttpAuthenticationMechanism '%s' provided no required credential types, therefore it needs "
                    + "to be able to perform authentication without any IdentityProvider", mechanism.getClass().getName());
            mechanisms.add(mechanism);
            return;
        }

        // mechanism requires an IdentityProvider, therefore we verify that such a provider exists
        boolean found = false;
        for (Class<? extends AuthenticationRequest> mechType : mechanism.getCredentialTypes()) {
            for (IdentityProvider<?> i : providers) {
                if (i.getRequestType().equals(mechType)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        if (found) {
            mechanisms.add(mechanism);
        } else if (BasicAuthenticationMechanism.class.equals(mechanism.getClass()) && basicAuthEnabled.isEmpty()) {
            LOG.debug("""
                    BasicAuthenticationMechanism has been enabled because no other authentication mechanism has been
                    detected, but there is no IdentityProvider based on username and password. Please use
                    one of supported extensions if you plan to use the mechanism.
                    For more information go to the https://quarkus.io/guides/security-basic-authentication-howto.
                    """);
        } else {
            throw new RuntimeException("""
                    HttpAuthenticationMechanism '%s' requires one or more IdentityProviders supporting at least one
                    of the following credentials types: %s.
                    Please refer to the https://quarkus.io/guides/security-identity-providers for more information.
                    """.formatted(mechanism.getClass().getName(), mechanism.getCredentialTypes()));
        }
    }

    private void addBasicAuthMechanismIfImplicitlyRequired(
            Instance<HttpAuthenticationMechanism> httpAuthenticationMechanism,
            List<HttpAuthenticationMechanism> mechanisms, Instance<IdentityProvider<?>> providers) {
        if (basicAuthEnabled.orElse(Boolean.FALSE)) {
            return;
        }
        if (!Boolean.getBoolean(TEST_IF_BASIC_AUTH_IMPLICITLY_REQUIRED) || isBasicAuthNotRequired()) {
            return;
        }

        var basicAuthMechInstance = httpAuthenticationMechanism.select(BasicAuthenticationMechanism.class);
        if (basicAuthMechInstance.isResolvable() && !mechanisms.contains(basicAuthMechInstance.get())) {
            for (IdentityProvider<?> i : providers) {
                if (UsernamePasswordAuthenticationRequest.class.equals(i.getRequestType())) {
                    mechanisms.add(basicAuthMechInstance.get());
                    return;
                }
            }
            LOG.debug("""
                    BasicAuthenticationMechanism has been enabled because no custom authentication mechanism has been detected
                    and basic authentication is required either by the HTTP Security Policy or '@BasicAuthentication', but
                    there is no IdentityProvider based on username and password. Please use one of supported extensions.
                    For more information, go to the https://quarkus.io/guides/security-basic-authentication-howto.
                    """);
        }
    }

    private static boolean isBasicAuthNotRequired() {
        if (Boolean.getBoolean(BASIC_AUTH_ANNOTATION_DETECTED)) {
            return false;
        }
        List<HttpSecurityConfiguration.HttpPermissionCarrier> httpPermissions = HttpSecurityConfiguration
                .get().httpPermissions();
        for (var permission : httpPermissions) {
            if (permission.getAuthMechanism() != null
                    && BasicAuthentication.AUTH_MECHANISM_SCHEME.equals(permission.getAuthMechanism().name())) {
                return false;
            }
        }
        return true;
    }
}
