package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHENTICATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHENTICATION_SUCCESS;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.DefaultAuthFailureHandler.DEV_MODE_AUTHENTICATION_FAILURE_BODY;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.SECURITY_IDENTITIES_ATTRIBUTE;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getSecurityIdentities;
import static io.quarkus.vertx.http.runtime.security.RolesMapping.ROLES_MAPPING_KEY;
import static io.quarkus.vertx.http.runtime.security.RoutingContextAwareSecurityIdentity.addRoutingCtxToIdentityIfMissing;
import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.vertx.http.runtime.AuthRuntimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.HttpSecurityConfiguration.AuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Class that is responsible for running the HTTP based authentication
 */
@Singleton
public final class HttpAuthenticator {
    /**
     * Special handling for the basic authentication mechanism, for user convenience, we add the mechanism when:
     * - not explicitly disabled or enabled
     * - is default bean and not programmatically looked up because there are other authentication mechanisms
     * - no custom auth mechanism is defined because then, we can't tell if user didn't provide custom impl.
     * - there is a provider that supports it (if not, we inform user via the log)
     * <p>
     * Presence of this system property means that we need to test whether:
     * - there are HTTP Permissions using explicitly this mechanism
     * - or {@link io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication}
     */
    public static final String TEST_IF_BASIC_AUTH_IMPLICITLY_REQUIRED = "io.quarkus.security.http.test-if-basic-auth-implicitly-required";
    /**
     * Whether {@link io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication} has been detected,
     * which means that user needs to use basic authentication.
     * Only set when detected and {@link HttpAuthenticator#TEST_IF_BASIC_AUTH_IMPLICITLY_REQUIRED} is true.
     */
    public static final String BASIC_AUTH_ANNOTATION_DETECTED = "io.quarkus.security.http.basic-authentication-annotation-detected";
    private static final Logger LOG = Logger.getLogger(HttpAuthenticator.class);
    /**
     * Added to a {@link RoutingContext} as selected authentication mechanism.
     */
    private static final String AUTH_MECHANISM = HttpAuthenticator.class.getName() + "#auth-mechanism";
    /**
     * Added to a {@link RoutingContext} when {@link this#attemptAuthentication(RoutingContext)} is invoked.
     */
    private static final String ATTEMPT_AUTH_INVOKED = HttpAuthenticator.class.getName() + "#attemptAuthentication";
    private static boolean selectAuthMechanismWithAnnotation = false;
    private final IdentityProviderManager identityProviderManager;
    private final HttpAuthenticationMechanism[] mechanisms;
    private final SecurityEventHelper<AuthenticationSuccessEvent, AuthenticationFailureEvent> securityEventHelper;
    private final boolean inclusiveAuth;
    private final boolean strictInclusiveMode;

    HttpAuthenticator(IdentityProviderManager identityProviderManager,
            Event<AuthenticationFailureEvent> authFailureEvent,
            Event<AuthenticationSuccessEvent> authSuccessEvent,
            BeanManager beanManager, VertxHttpBuildTimeConfig httpBuildTimeConfig,
            VertxHttpConfig httpConfig, Instance<HttpAuthenticationMechanism> httpAuthenticationMechanism,
            Instance<IdentityProvider<?>> providers,
            @ConfigProperty(name = "quarkus.security.events.enabled") boolean securityEventsEnabled) {
        this.securityEventHelper = new SecurityEventHelper<>(authSuccessEvent, authFailureEvent, AUTHENTICATION_SUCCESS,
                AUTHENTICATION_FAILURE, beanManager, securityEventsEnabled);
        this.identityProviderManager = identityProviderManager;
        this.inclusiveAuth = httpConfig.auth().inclusive();
        this.strictInclusiveMode = httpConfig.auth().inclusiveMode() == AuthRuntimeConfig.InclusiveMode.STRICT;
        List<HttpAuthenticationMechanism> mechanisms = new ArrayList<>();
        for (HttpAuthenticationMechanism mechanism : httpAuthenticationMechanism) {
            if (mechanism.getCredentialTypes().isEmpty()) {
                // mechanism does not require any IdentityProvider
                LOG.debugf("HttpAuthenticationMechanism '%s' provided no required credential types, therefore it needs "
                        + "to be able to perform authentication without any IdentityProvider", mechanism.getClass().getName());
                mechanisms.add(mechanism);
                continue;
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
            } else if (BasicAuthenticationMechanism.class.equals(mechanism.getClass())
                    && httpBuildTimeConfig.auth().basic().isEmpty()) {
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
        addBasicAuthMechanismIfImplicitlyRequired(httpAuthenticationMechanism, mechanisms, providers);
        if (mechanisms.isEmpty()) {
            this.mechanisms = new HttpAuthenticationMechanism[] { new NoAuthenticationMechanism() };
        } else {
            mechanisms.sort(new Comparator<HttpAuthenticationMechanism>() {
                @Override
                public int compare(HttpAuthenticationMechanism mech1, HttpAuthenticationMechanism mech2) {
                    //descending order
                    return Integer.compare(mech2.getPriority(), mech1.getPriority());
                }
            });
            this.mechanisms = mechanisms.toArray(new HttpAuthenticationMechanism[mechanisms.size()]);

            // if inclusive auth and mTLS are enabled, the mTLS must have the highest priority
            if (inclusiveAuth && Arc.container().instance(MtlsAuthenticationMechanism.class).isAvailable()) {
                var topMechanism = ClientProxy.unwrap(this.mechanisms[0]);
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
    }

    public IdentityProviderManager getIdentityProviderManager() {
        return identityProviderManager;
    }

    /**
     * Attempts authentication with the contents of the request. If this is possible the Uni
     * will resolve to a valid SecurityIdentity when it is subscribed to. Note that Uni is lazy,
     * so this may not happen until the Uni is subscribed to.
     * <p>
     * If invalid credentials are present then the completion stage will resolve to a
     * {@link io.quarkus.security.AuthenticationFailedException}
     * <p>
     * If no credentials are present it will resolve to null.
     */
    public Uni<SecurityIdentity> attemptAuthentication(RoutingContext routingContext) {
        // we need to keep track of authN attempts so that we know authN only happens after
        // the HTTP request has been matched with the annotated method
        if (selectAuthMechanismWithAnnotation) {
            rememberAuthAttempted(routingContext);
        }

        // determine whether user selected path specific mechanism via HTTP Security policy or annotation
        final AuthenticationMechanism pathSpecificMechanism;
        if (selectAuthMechanismWithAnnotation && isAuthMechanismSelected(routingContext)) {
            pathSpecificMechanism = getAuthMechanism(routingContext);
        } else {
            AbstractPathMatchingHttpSecurityPolicy pathMatchingPolicy = routingContext
                    .get(AbstractPathMatchingHttpSecurityPolicy.class.getName());
            pathSpecificMechanism = pathMatchingPolicy != null ? pathMatchingPolicy.getAuthMechanism(routingContext) : null;
        }

        // authenticate
        Uni<SecurityIdentity> result;
        if (pathSpecificMechanism == null) {
            result = createSecurityIdentity(routingContext, 0);
        } else {
            result = findBestCandidateMechanism(routingContext, pathSpecificMechanism).onItem().ifNotNull()
                    .transformToUni(new Function<HttpAuthenticationMechanism, Uni<? extends SecurityIdentity>>() {
                        @Override
                        public Uni<SecurityIdentity> apply(HttpAuthenticationMechanism mech) {
                            return mech.authenticate(routingContext, identityProviderManager);
                        }
                    });
        }

        if (inclusiveAuth && strictInclusiveMode && pathSpecificMechanism == null) {
            // inclusive authentication in the strict mode requires that all registered mechanisms created identity
            // if at least one of them created it (AKA: if identity is not null, null results in anonymous identity)
            // inclusive authentication is not applied when path-specific mechanism has been selected (because there
            // user said use 'xyz' mechanism, not all the mechanisms)
            result = result.onItem().ifNotNull()
                    .transformToUni(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                        @Override
                        public Uni<? extends SecurityIdentity> apply(SecurityIdentity identity) {
                            Map<String, SecurityIdentity> identities = HttpSecurityUtils.getSecurityIdentities(routingContext);
                            if (identities == null || identities.size() != mechanisms.length) {
                                return Uni.createFrom().failure(new AuthenticationFailedException(
                                        """
                                                There is '%d' HTTP authentication mechanisms, however only '%d' authentication mechanisms
                                                created identity: %s
                                                """
                                                .formatted(identities == null ? 0 : identities.size(), mechanisms.length,
                                                        identities == null ? "" : identities.keySet())));
                            }
                            return Uni.createFrom().item(identity);
                        }
                    });
        }

        if (routingContext.get(ROLES_MAPPING_KEY) != null) {
            result = result.onItem().ifNotNull().transform(routingContext.get(ROLES_MAPPING_KEY));
        }

        // fire security events if required
        if (securityEventHelper.fireEventOnFailure()) {
            result = result.onFailure().invoke(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    securityEventHelper.fireFailureEvent(new AuthenticationFailureEvent(throwable,
                            Map.of(RoutingContext.class.getName(), routingContext)));
                }
            });
        }
        if (securityEventHelper.fireEventOnSuccess()) {
            result = result.onItem().ifNotNull().invoke(new Consumer<SecurityIdentity>() {
                @Override
                public void accept(SecurityIdentity securityIdentity) {
                    securityEventHelper.fireSuccessEvent(new AuthenticationSuccessEvent(securityIdentity,
                            Map.of(RoutingContext.class.getName(), routingContext)));
                }
            });
        }

        return result;
    }

    private Uni<SecurityIdentity> createSecurityIdentity(RoutingContext routingContext, int i) {
        if (i == mechanisms.length) {
            return Uni.createFrom().nullItem();
        }
        return mechanisms[i].authenticate(routingContext, identityProviderManager)
                .onItem().transformToUni(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(SecurityIdentity identity) {
                        if (identity != null) {
                            if (inclusiveAuth) {
                                return authenticateWithAllMechanisms(identity, i, routingContext);
                            }
                            if (selectAuthMechanismWithAnnotation && !isAuthMechanismSelected(routingContext)) {
                                return rememberAuthMechScheme(mechanisms[i], routingContext).replaceWith(identity);
                            }
                            return Uni.createFrom().item(identity);
                        }
                        return createSecurityIdentity(routingContext, i + 1);
                    }
                });
    }

    /**
     * @return
     */
    public Uni<Boolean> sendChallenge(RoutingContext routingContext) {
        //we want to consume any body content if present
        //challenges won't read the body and didn't resume context themselves
        //as if we don't consume things can get stuck
        if (!routingContext.request().isEnded()) {
            routingContext.request().resume();
        }
        Uni<Boolean> result = null;

        // we only require auth mechanism to put itself into routing context when there is more than one mechanism registered
        if (mechanisms.length > 1) {
            HttpAuthenticationMechanism matchingMech = routingContext.get(HttpAuthenticationMechanism.class.getName());
            if (matchingMech != null) {
                result = matchingMech.sendChallenge(routingContext);
            }
        }

        if (result == null) {
            result = mechanisms[0].sendChallenge(routingContext);
            for (int i = 1; i < mechanisms.length; ++i) {
                HttpAuthenticationMechanism mech = mechanisms[i];
                result = result.onItem().transformToUni(new Function<Boolean, Uni<? extends Boolean>>() {
                    @Override
                    public Uni<? extends Boolean> apply(Boolean authDone) {
                        if (authDone) {
                            return Uni.createFrom().item(authDone);
                        }
                        return mech.sendChallenge(routingContext);
                    }
                });
            }
        }
        return result.onItem().transformToUni(new Function<Boolean, Uni<? extends Boolean>>() {
            @Override
            public Uni<? extends Boolean> apply(Boolean authDone) {
                if (!authDone) {
                    LOG.debug("Authentication has not been done, returning HTTP status 401");
                    routingContext.response().setStatusCode(401);
                    if (routingContext.get(DEV_MODE_AUTHENTICATION_FAILURE_BODY) == null) {
                        routingContext.response().end();
                    } else {
                        final String authenticationFailureBody = routingContext.get(DEV_MODE_AUTHENTICATION_FAILURE_BODY);
                        routingContext.response().end(authenticationFailureBody);
                    }
                }
                return Uni.createFrom().item(authDone);
            }
        });
    }

    public Uni<ChallengeData> getChallenge(RoutingContext routingContext) {
        // we only require auth mechanism to put itself into routing context when there is more than one mechanism registered
        if (mechanisms.length > 1) {
            HttpAuthenticationMechanism matchingMech = routingContext.get(HttpAuthenticationMechanism.class.getName());
            if (matchingMech != null) {
                return matchingMech.getChallenge(routingContext);
            }
        }
        Uni<ChallengeData> result = mechanisms[0].getChallenge(routingContext);
        for (int i = 1; i < mechanisms.length; ++i) {
            HttpAuthenticationMechanism mech = mechanisms[i];
            result = result.onItem().transformToUni(new Function<ChallengeData, Uni<? extends ChallengeData>>() {
                @Override
                public Uni<? extends ChallengeData> apply(ChallengeData data) {
                    if (data != null) {
                        return Uni.createFrom().item(data);
                    }
                    return mech.getChallenge(routingContext);
                }
            });

        }
        return result;
    }

    private Uni<SecurityIdentity> authenticateWithAllMechanisms(SecurityIdentity identity, int i,
            RoutingContext routingContext) {
        return mechanisms[i].getCredentialTransport(routingContext)
                .onItem().transformToUni(new Function<HttpCredentialTransport, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(HttpCredentialTransport httpCredentialTransport) {
                        if (httpCredentialTransport == null || httpCredentialTransport.getAuthenticationScheme() == null) {
                            LOG.error("""
                                    Illegal state - HttpAuthenticationMechanism '%s' authentication scheme is not available.
                                    The authentication scheme is required when inclusive authentication is enabled.
                                    """.formatted(ClientProxy.unwrap(mechanisms[i]).getClass().getName()));
                            return Uni.createFrom().failure(new AuthenticationFailedException());
                        }
                        var authMechanism = httpCredentialTransport.getAuthenticationScheme();

                        // add current identity to the RoutingContext
                        var authMechToIdentity = getSecurityIdentities(routingContext);
                        boolean isFirstIdentity = authMechToIdentity == null;
                        if (isFirstIdentity) {
                            authMechToIdentity = new HashMap<>();
                            routingContext.put(SECURITY_IDENTITIES_ATTRIBUTE, authMechToIdentity);
                        }
                        authMechToIdentity.putIfAbsent(authMechanism, identity);

                        // authenticate with remaining mechanisms
                        if (isFirstIdentity) {
                            return createSecurityIdentity(routingContext, i + 1)
                                    .replaceWith(addRoutingCtxToIdentityIfMissing(identity, routingContext));
                        } else {
                            return createSecurityIdentity(routingContext, i + 1);
                        }
                    }
                });
    }

    private Uni<HttpAuthenticationMechanism> findBestCandidateMechanism(RoutingContext routingContext,
            AuthenticationMechanism pathSpecificMechanism) {
        if (pathSpecificMechanism.instance() != null) {
            rememberAuthMechanism(routingContext, pathSpecificMechanism);
            return Uni.createFrom().item(pathSpecificMechanism.instance());
        }
        return findBestCandidateMechanism(routingContext, pathSpecificMechanism.name(), 0);
    }

    private Uni<HttpAuthenticationMechanism> findBestCandidateMechanism(RoutingContext routingContext,
            String pathSpecificMechanism, int i) {
        if (i == mechanisms.length) {
            return Uni.createFrom().nullItem();
        }
        return getPathSpecificMechanism(i, routingContext, pathSpecificMechanism).onItem().transformToUni(
                new Function<HttpAuthenticationMechanism, Uni<? extends HttpAuthenticationMechanism>>() {
                    @Override
                    public Uni<? extends HttpAuthenticationMechanism> apply(HttpAuthenticationMechanism mech) {
                        if (mech != null) {
                            if (selectAuthMechanismWithAnnotation && !isAuthMechanismSelected(routingContext)) {
                                return rememberAuthMechScheme(mech, routingContext).replaceWith(mech);
                            }
                            return Uni.createFrom().item(mech);
                        }
                        return findBestCandidateMechanism(routingContext, pathSpecificMechanism, i + 1);
                    }
                });
    }

    private Uni<HttpAuthenticationMechanism> getPathSpecificMechanism(int index, RoutingContext routingContext,
            String pathSpecificMechanism) {
        return mechanisms[index].getCredentialTransport(routingContext).onItem()
                .transform(new Function<HttpCredentialTransport, HttpAuthenticationMechanism>() {
                    @Override
                    public HttpAuthenticationMechanism apply(HttpCredentialTransport t) {
                        if (t != null && t.getAuthenticationScheme().equalsIgnoreCase(pathSpecificMechanism)) {
                            routingContext.put(HttpAuthenticationMechanism.class.getName(), mechanisms[index]);
                            rememberAuthMechanism(routingContext, t.getAuthenticationScheme());
                            return mechanisms[index];
                        }
                        return null;
                    }
                });
    }

    static void selectAuthMechanismWithAnnotation() {
        selectAuthMechanismWithAnnotation = true;
    }

    static void selectAuthMechanism(RoutingContext routingContext, String authMechanism) {
        if (requestAlreadyAuthenticated(routingContext, authMechanism)) {
            AuthenticationMechanism authenticationMechanism = getAuthMechanism(routingContext);
            final String previousMechanism;
            if (authenticationMechanism != null) {
                if (authenticationMechanism.name() != null) {
                    previousMechanism = authenticationMechanism.name();
                } else {
                    previousMechanism = ClientProxy.unwrap(authenticationMechanism.instance()).getClass().getName();
                }
            } else {
                previousMechanism = null;
            }
            throw new AuthenticationFailedException("""
                    The '%1$s' authentication mechanism is required to authenticate the request but it was already
                    authenticated with the '%2$s' authentication mechanism. It can happen if the '%1$s' is selected with
                    an annotation but '%2$s' is activated by the HTTP security policy which is enforced before
                    the JAX-RS chain is run. In such cases, please set the
                    'quarkus.http.auth.permission."permissions".applies-to=JAXRS' to all HTTP security policies
                    which secure the same REST endpoints as the ones secured by the '%1$s' authentication mechanism
                    selected with the annotation.
                    """.formatted(authMechanism, previousMechanism));
        }
        rememberAuthMechanism(routingContext, authMechanism);
    }

    private static void rememberAuthAttempted(RoutingContext routingContext) {
        routingContext.put(ATTEMPT_AUTH_INVOKED, TRUE);
    }

    private static boolean isAuthMechanismSelected(RoutingContext routingContext) {
        return getAuthMechanism(routingContext) != null;
    }

    private static boolean requestAlreadyAuthenticated(RoutingContext event, String newAuthMechanism) {
        return event.get(ATTEMPT_AUTH_INVOKED) == TRUE && authenticatedWithDifferentAuthMechanism(newAuthMechanism, event);
    }

    private static boolean authenticatedWithDifferentAuthMechanism(String newAuthMechanism, RoutingContext event) {
        return !newAuthMechanism.equalsIgnoreCase(getAuthMechanismScheme(event));
    }

    /**
     * Remember authentication mechanism used for authentication so that we know what mechanism has been used
     * in case that someone tries to change the mechanism after the authentication. This way, we can be permissive
     * when the selected mechanism is same as the one already used.
     */
    private static Uni<HttpCredentialTransport> rememberAuthMechScheme(HttpAuthenticationMechanism mech, RoutingContext event) {
        return mech.getCredentialTransport(event)
                .onItem().ifNotNull().invoke(new Consumer<HttpCredentialTransport>() {
                    @Override
                    public void accept(HttpCredentialTransport t) {
                        if (t.getAuthenticationScheme() != null) {
                            rememberAuthMechanism(event, t.getAuthenticationScheme());
                        }
                    }
                });
    }

    private static void rememberAuthMechanism(RoutingContext event, AuthenticationMechanism newAuthMechanism) {
        event.put(AUTH_MECHANISM, newAuthMechanism);
        event.put(HttpAuthenticationMechanism.class.getName(), newAuthMechanism.instance());
    }

    private static void rememberAuthMechanism(RoutingContext event, String newAuthMechanism) {
        event.put(AUTH_MECHANISM, new AuthenticationMechanism(newAuthMechanism, null));
    }

    private static AuthenticationMechanism getAuthMechanism(RoutingContext event) {
        return event.get(AUTH_MECHANISM);
    }

    private static String getAuthMechanismScheme(RoutingContext event) {
        AuthenticationMechanism authenticationMechanism = getAuthMechanism(event);
        if (authenticationMechanism != null) {
            return authenticationMechanism.name();
        }
        return null;
    }

    private static void addBasicAuthMechanismIfImplicitlyRequired(
            Instance<HttpAuthenticationMechanism> httpAuthenticationMechanism,
            List<HttpAuthenticationMechanism> mechanisms, Instance<IdentityProvider<?>> providers) {
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
        VertxHttpConfig httpConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
                .getConfigMapping(VertxHttpConfig.class);
        List<HttpSecurityConfiguration.HttpPermissionCarrier> httpPermissions = HttpSecurityConfiguration
                .get(httpConfig).httpPermissions();
        for (var permission : httpPermissions) {
            if (permission.getAuthMechanism() != null
                    && BasicAuthentication.AUTH_MECHANISM_SCHEME.equals(permission.getAuthMechanism().name())) {
                return false;
            }
        }
        return true;
    }

    static class NoAuthenticationMechanism implements HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context,
                IdentityProviderManager identityProviderManager) {
            return Uni.createFrom().optional(Optional.empty());
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            ChallengeData challengeData = new ChallengeData(HttpResponseStatus.FORBIDDEN.code(), null, null);
            return Uni.createFrom().item(challengeData);
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return Collections.singleton(AnonymousAuthenticationRequest.class);
        }

    }

}
