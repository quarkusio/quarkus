package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.options.HttpServerTlsConfig.getTlsClientAuth;
import static io.quarkus.vertx.http.runtime.security.HttpAuthenticator.BASIC_AUTH_ANNOTATION_DETECTED;
import static io.quarkus.vertx.http.runtime.security.HttpAuthenticator.TEST_IF_BASIC_AUTH_IMPLICITLY_REQUIRED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.vertx.http.runtime.AuthRuntimeConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.quarkus.vertx.http.runtime.options.HttpServerTlsConfig;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.security.CSRF;
import io.quarkus.vertx.http.security.HttpSecurity;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.http.ClientAuth;

/**
 * This singleton carries final HTTP Security configuration and act as a single source of truth for it.
 * This class is not part of the public API and is subject to change.
 */
public final class HttpSecurityConfiguration {

    private static final Logger LOG = Logger.getLogger(HttpSecurityConfiguration.class);
    private static volatile HttpSecurityConfiguration instance = null;
    private final RolesMapping rolesMapping;
    private final List<HttpPermissionCarrier> httpPermissions;
    private final Optional<Boolean> basicAuthEnabled;
    private final boolean formAuthEnabled;
    private final String formPostLocation;
    private final List<HttpAuthenticationMechanism> additionalMechanisms;
    private final VertxHttpConfig httpConfig;
    private final VertxHttpBuildTimeConfig httpBuildTimeConfig;
    private final CORSConfig corsConfig;
    private final CSRF csrf;

    private HttpSecurityConfiguration(RolesMapping rolesMapping, List<HttpPermissionCarrier> httpPermissions,
            Optional<Boolean> basicAuthEnabled, boolean formAuthEnabled, String formPostLocation,
            List<HttpAuthenticationMechanism> additionalMechanisms, VertxHttpConfig httpConfig,
            VertxHttpBuildTimeConfig httpBuildTimeConfig, CORSConfig corsConfig, CSRF csrf) {
        this.rolesMapping = rolesMapping;
        this.httpPermissions = httpPermissions;
        this.basicAuthEnabled = basicAuthEnabled;
        this.formAuthEnabled = formAuthEnabled;
        this.formPostLocation = formPostLocation;
        this.additionalMechanisms = additionalMechanisms;
        this.httpConfig = httpConfig;
        this.httpBuildTimeConfig = httpBuildTimeConfig;
        this.corsConfig = corsConfig;
        this.csrf = csrf;
    }

    record Policy(String name, HttpSecurityPolicy instance) {
    }

    record AuthenticationMechanisms(Set<String> names) {

        AuthenticationMechanisms(String... names) {
            this(normalizeNames(names));
        }

        AuthenticationMechanisms with(String newName) {
            var newNames = new HashSet<>(names);
            newNames.add(normalizeMechanismName(newName));
            return new AuthenticationMechanisms(Collections.unmodifiableSet(newNames));
        }

        /**
         * Unlike the default constructor, this factory method normalizes given mechanism names.
         *
         * @param names authentication mechanism names
         * @return new AuthenticationMechanisms
         */
        static AuthenticationMechanisms from(Set<String> names) {
            return new AuthenticationMechanisms(names.stream().map(AuthenticationMechanisms::normalizeMechanismName)
                    .collect(Collectors.toUnmodifiableSet()));
        }

        static String normalizeMechanismName(String mechanismName) {
            if (mechanismName == null) {
                return "";
            }
            return mechanismName.toLowerCase(Locale.ROOT);
        }

        private static Set<String> normalizeNames(String[] names) {
            return names.length == 1 ? Set.of(normalizeMechanismName(names[0]))
                    : Arrays.stream(names).map(AuthenticationMechanisms::normalizeMechanismName)
                            .collect(Collectors.toUnmodifiableSet());
        }
    }

    interface HttpPermissionCarrier {

        Set<String> getPaths();

        boolean isShared();

        boolean shouldApplyToJaxRs();

        Set<String> getMethods();

        AuthenticationMechanisms getAuthMechanisms();

        Policy getPolicy();

        default PolicyMappingConfig.AppliesTo getAppliesTo() {
            return shouldApplyToJaxRs() ? PolicyMappingConfig.AppliesTo.JAXRS : PolicyMappingConfig.AppliesTo.ALL;
        }
    }

    BasicAuthenticationMechanism getBasicAuthenticationMechanism() {
        for (HttpAuthenticationMechanism additionalMechanism : additionalMechanisms) {
            if (additionalMechanism.getClass() == BasicAuthenticationMechanism.class) {
                return (BasicAuthenticationMechanism) additionalMechanism;
            }
        }
        return new BasicAuthenticationMechanism(httpConfig.auth().realm().orElse(null), formAuthEnabled,
                httpConfig.auth().basicPriority());
    }

    FormAuthenticationMechanism getFormAuthenticationMechanism() {
        for (HttpAuthenticationMechanism additionalMechanism : additionalMechanisms) {
            if (additionalMechanism.getClass() == FormAuthenticationMechanism.class) {
                return (FormAuthenticationMechanism) additionalMechanism;
            }
        }
        return new FormAuthenticationMechanism(httpConfig.auth().form(), httpConfig.encryptionKey());
    }

    MtlsAuthenticationMechanism getMtlsAuthenticationMechanism() {
        if (ClientAuth.NONE.equals(getTlsClientAuth(httpConfig, httpBuildTimeConfig, LaunchMode.current()))) {
            return null;
        }
        for (HttpAuthenticationMechanism additionalMechanism : additionalMechanisms) {
            if (additionalMechanism.getClass() == MtlsAuthenticationMechanism.class) {
                return (MtlsAuthenticationMechanism) additionalMechanism;
            }
        }
        var mTLS = Arc.container().select(MtlsAuthenticationMechanism.class).orNull();
        if (mTLS == null) {
            // this would be a bug in Quarkus, nothing for users to do
            throw new IllegalStateException("TLS client authentication mechanism is required but no "
                    + "HttpAuthenticationMechanism which supports it was found");
        }
        return mTLS;
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
            if (inclusiveAuth && getMtlsAuthenticationMechanism() != null) {
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

    static HttpSecurityConfiguration get() {
        return get(null, null);
    }

    // this instance is not in the CDI container to avoid "potential" (I am guessing) circular dependencies
    // during the bean instantiation as we can't be sure what users will inject when they observe the HTTP Security
    static HttpSecurityConfiguration get(VertxHttpConfig httpConfig, VertxHttpBuildTimeConfig httpBuildTimeConfig) {
        var configInstance = instance;
        if (configInstance == null) {
            return initializeHttpSecurityConfiguration(httpConfig, httpBuildTimeConfig);
        }
        return configInstance;
    }

    static void clear() {
        instance = null;
        HttpServerTlsConfig.setConfiguration(new ProgrammaticTlsConfig(null, Optional.empty()));
    }

    private static synchronized HttpSecurityConfiguration initializeHttpSecurityConfiguration(VertxHttpConfig httpConfig,
            VertxHttpBuildTimeConfig httpBuildTimeConfig) {
        if (instance == null) {
            final VertxHttpConfig vertxHttpConfig;
            final VertxHttpBuildTimeConfig vertxHttpBuildTimeConfig;
            if (httpConfig == null) {
                SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
                vertxHttpConfig = config.getConfigMapping(VertxHttpConfig.class);
                vertxHttpBuildTimeConfig = config.getConfigMapping(VertxHttpBuildTimeConfig.class);
            } else {
                vertxHttpConfig = httpConfig;
                vertxHttpBuildTimeConfig = Objects.requireNonNull(httpBuildTimeConfig);
            }
            HttpSecurityImpl httpSecurity = prepareHttpSecurity(vertxHttpConfig, vertxHttpBuildTimeConfig.tlsClientAuth());
            List<HttpAuthenticationMechanism> mechanisms = httpSecurity.getMechanisms();

            Optional<Boolean> basicAuthEnabled = vertxHttpBuildTimeConfig.auth().basic();
            if (basicAuthEnabled.isEmpty() || !basicAuthEnabled.get()) {
                for (HttpAuthenticationMechanism mechanism : mechanisms) {
                    // not using instance of as we are not considering subclasses
                    if (mechanism.getClass() == BasicAuthenticationMechanism.class) {
                        basicAuthEnabled = Optional.of(Boolean.TRUE);
                        break;
                    }
                }
            }

            boolean formAuthEnabled = vertxHttpBuildTimeConfig.auth().form();
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
                    basicAuthEnabled, formAuthEnabled, formPostLocation, mechanisms, vertxHttpConfig,
                    vertxHttpBuildTimeConfig, httpSecurity.getCorsConfig(), httpSecurity.getCsrf());
            HttpServerTlsConfig.setConfiguration(
                    new ProgrammaticTlsConfig(httpSecurity.getClientAuth(), httpSecurity.getHttpServerTlsConfigName()));
        }
        return instance;
    }

    private static HttpSecurityImpl prepareHttpSecurity(VertxHttpConfig httpConfig, ClientAuth clientAuth) {
        HttpSecurityImpl httpSecurity = new HttpSecurityImpl(clientAuth, httpConfig, httpConfig.tlsConfigurationName());
        addAuthRuntimeConfigToHttpSecurity(httpConfig.auth(), httpSecurity);
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
            public AuthenticationMechanisms getAuthMechanisms() {
                if (mapping.authMechanism().isPresent()) {
                    var mechanisms = mapping.authMechanism().get();
                    if (!mechanisms.isEmpty()) {
                        return AuthenticationMechanisms.from(mechanisms);
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

    private boolean isBasicAuthNotRequired() {
        if (Boolean.getBoolean(BASIC_AUTH_ANNOTATION_DETECTED)) {
            return false;
        }
        for (var permission : httpPermissions) {
            if (permission.getAuthMechanisms() != null
                    && permission.getAuthMechanisms().names().contains(BasicAuthentication.AUTH_MECHANISM_SCHEME)) {
                return false;
            }
        }
        return true;
    }

    RolesMapping rolesMapping() {
        return rolesMapping;
    }

    List<HttpPermissionCarrier> httpPermissions() {
        return httpPermissions;
    }

    boolean formAuthEnabled() {
        return formAuthEnabled;
    }

    String formPostLocation() {
        return formPostLocation;
    }

    CORSConfig getCorsConfig() {
        return corsConfig;
    }

    /**
     * This method allows to assure from outside of this HTTP Security package that HTTP Security config is ready.
     * It should be only used in very special cases, like to assure that programmatic TLS configuration and TLS client
     * authentication are loaded. It is unnecessary to use this method inside this HTTP Security package.
     *
     * @return true if programmatic configuration is ready
     */
    public static boolean isNotReady(VertxHttpConfig httpConfig, VertxHttpBuildTimeConfig httpBuildTimeConfig,
            LaunchMode launchMode) {
        if (instance != null) {
            return false;
        }

        var container = Arc.container();
        if (container == null) {
            if (launchMode == LaunchMode.DEVELOPMENT) {
                // there is one exception when we know that CDI container can be null and that is when server is starting
                // after failed start (e.g. compilation error was fixed); we document this known limitation and it is
                // only relevant for TLS config and TLS client auth, we must fail for everything else
                return true;
            } else {
                throw new IllegalStateException(
                        "CDI container is not available, cannot initialize HTTP Security configuration");
            }
        } else if (isHttpSecurityEventNotObserved(container)) {
            return false;
        }

        get(httpConfig, httpBuildTimeConfig);
        return false;
    }

    public static CSRF getProgrammaticCsrfConfig(VertxHttpConfig httpConfig, VertxHttpBuildTimeConfig httpBuildTimeConfig) {
        var container = Arc.container();
        if (container == null || isHttpSecurityEventNotObserved(container)) {
            // return null for example if the security extension is not present, or if user doesn't use the HttpSecurity
            return null;
        }
        return get(httpConfig, httpBuildTimeConfig).csrf;
    }

    private static boolean isHttpSecurityEventNotObserved(ArcContainer container) {
        return container.beanManager().resolveObserverMethods(new HttpSecurityImpl(null, null, Optional.empty())).isEmpty();
    }

    public static final class ProgrammaticTlsConfig {
        public final ClientAuth tlsClientAuth;
        public final Optional<String> tlsConfigName;

        private ProgrammaticTlsConfig(ClientAuth tlsClientAuth, Optional<String> tlsConfigName) {
            this.tlsClientAuth = tlsClientAuth;
            this.tlsConfigName = Objects.requireNonNull(tlsConfigName);
        }
    }
}
