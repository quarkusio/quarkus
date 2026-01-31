package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHENTICATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHENTICATION_SUCCESS;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityConfiguration.AuthenticationMechanisms.normalizeMechanismName;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.DefaultAuthFailureHandler.DEV_MODE_AUTHENTICATION_FAILURE_BODY;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.SECURITY_IDENTITIES_ATTRIBUTE;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getSecurityIdentities;
import static io.quarkus.vertx.http.runtime.security.RolesMapping.ROLES_MAPPING_KEY;
import static io.quarkus.vertx.http.runtime.security.RoutingContextAwareSecurityIdentity.addRoutingCtxToIdentityIfMissing;
import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.arc.ClientProxy;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.vertx.http.runtime.AuthRuntimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.HttpSecurityConfiguration.AuthenticationMechanisms;
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
     * Added to a {@link RoutingContext} as selected authentication mechanisms of type {@link AuthenticationMechanisms}.
     */
    private static final String SELECTED_AUTH_MECHANISMS = HttpAuthenticator.class.getName() + "#selected-auth-mechanisms";
    /**
     * Added to a {@link RoutingContext} as selected {@link HttpAuthenticationMechanism} instances.
     */
    private static final String SELECTED_AUTH_MECHANISM_INSTANCES = HttpAuthenticator.class.getName()
            + "#selected-auth-mechanism-instances";
    /**
     * Added to a {@link RoutingContext} when {@link this#attemptAuthentication(RoutingContext)} is invoked.
     */
    private static final String ATTEMPT_AUTH_INVOKED = HttpAuthenticator.class.getName() + "#attemptAuthentication";
    private static boolean selectAuthMechanismWithAnnotation = false;
    private final IdentityProviderManager identityProviderManager;
    private final HttpAuthenticationMechanism[] mechanisms;
    private final SecurityEventHelper<AuthenticationSuccessEvent, AuthenticationFailureEvent> securityEventHelper;
    private final boolean globalInclusiveAuth;
    private final boolean strictInclusiveMode;

    HttpAuthenticator(IdentityProviderManager identityProviderManager, Event<AuthenticationFailureEvent> authFailureEvent,
            Event<AuthenticationSuccessEvent> authSuccessEvent, BeanManager beanManager,
            VertxHttpConfig httpConfig, Instance<IdentityProvider<?>> providers,
            @ConfigProperty(name = "quarkus.security.events.enabled") boolean securityEventsEnabled) {
        this.securityEventHelper = new SecurityEventHelper<>(authSuccessEvent, authFailureEvent, AUTHENTICATION_SUCCESS,
                AUTHENTICATION_FAILURE, beanManager, securityEventsEnabled);
        this.identityProviderManager = identityProviderManager;
        this.globalInclusiveAuth = httpConfig.auth().inclusive();
        this.strictInclusiveMode = httpConfig.auth().inclusiveMode() == AuthRuntimeConfig.InclusiveMode.STRICT;
        this.mechanisms = HttpSecurityConfiguration.get().getMechanisms(providers, globalInclusiveAuth);
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

        // determine whether user selected path specific mechanisms via HTTP Security policy or annotation
        final AuthenticationMechanisms pathSpecificMechanisms;
        if (selectAuthMechanismWithAnnotation && areAuthMechanismsSelected(routingContext)) {
            pathSpecificMechanisms = getSelectedAuthMechanisms(routingContext);
        } else {
            AbstractPathMatchingHttpSecurityPolicy pathMatchingPolicy = routingContext
                    .get(AbstractPathMatchingHttpSecurityPolicy.class.getName());
            pathSpecificMechanisms = pathMatchingPolicy != null ? pathMatchingPolicy.getAuthMechanisms(routingContext) : null;
        }

        // authenticate
        Uni<SecurityIdentity> result;
        if (pathSpecificMechanisms == null) {
            result = createAndValidateSecurityIdentity(routingContext, mechanisms, identityProviderManager, globalInclusiveAuth,
                    strictInclusiveMode, mechanisms.length);
        } else {
            result = findBestCandidateMechanisms(routingContext, 0, new HashSet<>(pathSpecificMechanisms.names()),
                    new LinkedList<>()).onItem().ifNotNull()
                    .transformToUni(new Function<HttpAuthenticationMechanism[], Uni<? extends SecurityIdentity>>() {
                        @Override
                        public Uni<SecurityIdentity> apply(HttpAuthenticationMechanism[] candidates) {
                            return createAndValidateSecurityIdentity(routingContext, candidates, identityProviderManager,
                                    globalInclusiveAuth, strictInclusiveMode, pathSpecificMechanisms.names().size());
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

    private static Uni<SecurityIdentity> createAndValidateSecurityIdentity(RoutingContext routingContext,
            HttpAuthenticationMechanism[] mechanisms, IdentityProviderManager identityProviderManager, boolean inclusiveAuth,
            boolean strictInclusiveMode, int expectedIdentitiesCount) {
        if (inclusiveAuth && strictInclusiveMode) {
            // inclusive authentication in the strict mode requires that all selected mechanisms created identity
            // if at least one of them created it (AKA: if identity is not null, null results in anonymous identity)
            return createSecurityIdentity(routingContext, 0, mechanisms, identityProviderManager, true)
                    .onItem().ifNotNull()
                    .transformToUni(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                        @Override
                        public Uni<? extends SecurityIdentity> apply(SecurityIdentity identity) {
                            Map<String, SecurityIdentity> identities = HttpSecurityUtils.getSecurityIdentities(routingContext);
                            if (identities == null || identities.size() != expectedIdentitiesCount) {
                                return Uni.createFrom().failure(new AuthenticationFailedException(
                                        """
                                                There is '%d' selected HTTP authentication mechanisms, however only '%d'
                                                authentication mechanisms created identity: %s
                                                """
                                                .formatted(expectedIdentitiesCount, identities == null ? 0 : identities.size(),
                                                        identities == null ? "" : identities.keySet())));
                            }
                            return Uni.createFrom().item(identity);
                        }
                    });
        } else {
            return createSecurityIdentity(routingContext, 0, mechanisms, identityProviderManager, inclusiveAuth);
        }
    }

    private static Uni<SecurityIdentity> createSecurityIdentity(RoutingContext routingContext, int i,
            HttpAuthenticationMechanism[] mechanisms, IdentityProviderManager identityProviderManager, boolean inclusiveAuth) {
        if (i == mechanisms.length) {
            return Uni.createFrom().nullItem();
        }
        return mechanisms[i].authenticate(routingContext, identityProviderManager)
                .onItem().transformToUni(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(SecurityIdentity identity) {
                        if (identity != null) {
                            if (inclusiveAuth) {
                                return authenticateWithAllMechanisms(identity, i, routingContext, mechanisms,
                                        identityProviderManager);
                            }
                            if (selectAuthMechanismWithAnnotation && !areAuthMechanismsSelected(routingContext)) {
                                // this is done so that we can recognize if authentication happened before
                                // the mechanism was selected with the annotation, however, the authentication happened
                                // using the correct mechanism, therefore it is not illegal state (we can be lenient)
                                return rememberSelectedAuthMechScheme(mechanisms[i], routingContext).replaceWith(identity);
                            }
                            return Uni.createFrom().item(identity);
                        }
                        return createSecurityIdentity(routingContext, i + 1, mechanisms, identityProviderManager,
                                inclusiveAuth);
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

        HttpAuthenticationMechanism[] challengeMechanisms = getChallengeMechanisms(routingContext);
        Uni<Boolean> result = challengeMechanisms[0].sendChallenge(routingContext);
        for (int i = 1; i < challengeMechanisms.length; ++i) {
            HttpAuthenticationMechanism mech = challengeMechanisms[i];
            result = result.onItem().transformToUni(new Function<Boolean, Uni<? extends Boolean>>() {
                @Override
                public Uni<? extends Boolean> apply(Boolean authDone) {
                    if (Boolean.TRUE.equals(authDone)) {
                        return Uni.createFrom().item(true);
                    }
                    return mech.sendChallenge(routingContext);
                }
            });
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
        HttpAuthenticationMechanism[] challengeMechanisms = getChallengeMechanisms(routingContext);
        Uni<ChallengeData> result = challengeMechanisms[0].getChallenge(routingContext);
        for (int i = 1; i < challengeMechanisms.length; ++i) {
            HttpAuthenticationMechanism mech = challengeMechanisms[i];
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

    private HttpAuthenticationMechanism[] getChallengeMechanisms(RoutingContext routingContext) {
        HttpAuthenticationMechanism[] challengeMechanisms;

        // this is mechanism that added itself to the RoutingContext last in one of following situations:
        // - it started to authenticate using this HTTP authenticator and authentication failed or redirect is needed
        // - some mechanisms can be used directly without this HttpAuthenticator instance
        //   like when user calls form-based mechanism's post location
        // - someone can invoke challenge even if authentication succeeded, but we don't expect that to happen
        HttpAuthenticationMechanism directlyUsedAuthenticationMechanism = routingContext
                .get(HttpAuthenticationMechanism.class.getName());

        if (directlyUsedAuthenticationMechanism != null) {

            challengeMechanisms = new HttpAuthenticationMechanism[] { directlyUsedAuthenticationMechanism };
        } else {
            // either no authentication mechanism provided a SecurityIdentity, or some custom mechanism
            // provided it, but did not add itself into the RoutingContext

            // path-specific mechanisms
            challengeMechanisms = getSelectedAuthMechanismInstances(routingContext);

            if (challengeMechanisms == null) {
                // if no path-specific mechanism was selected, we use all the authentication mechanisms
                challengeMechanisms = mechanisms;
            }
        }

        return challengeMechanisms;
    }

    private static Uni<SecurityIdentity> authenticateWithAllMechanisms(SecurityIdentity identity, int i,
            RoutingContext routingContext, HttpAuthenticationMechanism[] mechanisms,
            IdentityProviderManager identityProviderManager) {
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
                        rememberSelectedAuthMechanism(routingContext, authMechanism);

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
                            return createSecurityIdentity(routingContext, i + 1, mechanisms, identityProviderManager, true)
                                    .replaceWith(addRoutingCtxToIdentityIfMissing(identity, routingContext));
                        } else {
                            return createSecurityIdentity(routingContext, i + 1, mechanisms, identityProviderManager, true);
                        }
                    }
                });
    }

    private Uni<HttpAuthenticationMechanism[]> findBestCandidateMechanisms(RoutingContext routingContext, int i,
            Set<String> mechanismsToFind, List<HttpAuthenticationMechanism> foundSelectedMechanisms) {
        if (i == mechanisms.length) {
            if (foundSelectedMechanisms.isEmpty()) {
                return Uni.createFrom().nullItem();
            }
            return Uni.createFrom().item(foundSelectedMechanisms.toArray(HttpAuthenticationMechanism[]::new));
        }
        return getPathSpecificMechanism(i, routingContext, mechanismsToFind).flatMap(
                new Function<HttpAuthenticationMechanism, Uni<? extends HttpAuthenticationMechanism[]>>() {
                    @Override
                    public Uni<? extends HttpAuthenticationMechanism[]> apply(HttpAuthenticationMechanism mech) {
                        if (mech != null) {
                            foundSelectedMechanisms.add(mech);
                            if (mechanismsToFind.isEmpty()) {
                                return Uni.createFrom()
                                        .item(foundSelectedMechanisms.toArray(HttpAuthenticationMechanism[]::new));
                            }
                        }
                        return findBestCandidateMechanisms(routingContext, i + 1, mechanismsToFind, foundSelectedMechanisms);
                    }
                });
    }

    private Uni<HttpAuthenticationMechanism> getPathSpecificMechanism(int index, RoutingContext routingContext,
            Set<String> mechanismsToFind) {
        return mechanisms[index].getCredentialTransport(routingContext).onItem()
                .transform(new Function<HttpCredentialTransport, HttpAuthenticationMechanism>() {
                    @Override
                    public HttpAuthenticationMechanism apply(HttpCredentialTransport t) {
                        if (t != null && mechanismsToFind.contains(normalizeMechanismName(t.getAuthenticationScheme()))) {
                            addSelectedAuthMechanismInstance(mechanisms[index], routingContext);
                            rememberSelectedAuthMechanism(routingContext, t.getAuthenticationScheme());
                            mechanismsToFind.remove(t.getAuthenticationScheme());
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
        if (authenticationAlreadyAttempted(routingContext, authMechanism)) {
            AuthenticationMechanisms authenticationMechanisms = getSelectedAuthMechanisms(routingContext);
            final String previousMechanisms;
            if (authenticationMechanisms != null) {
                previousMechanisms = authenticationMechanisms.names().toString();
            } else {
                previousMechanisms = "";
            }
            throw new AuthenticationFailedException("""
                    The '%1$s' authentication mechanism is required to authenticate the request but it was already
                    authenticated with the '%2$s' authentication mechanisms.
                    It can happen if the '%1$s' is selected with an annotation but '%2$s' are activated by
                    the HTTP security policy which is enforced before the JAX-RS chain is run. In such cases, please
                    set the 'quarkus.http.auth.permission."permissions".applies-to=JAXRS' to all HTTP security policies
                    which secure the same REST endpoints as the ones secured by the '%1$s' authentication mechanism
                    selected with the annotation.
                    """.formatted(authMechanism, previousMechanisms));
        }
        rememberSelectedAuthMechanism(routingContext, authMechanism);
    }

    private static void rememberAuthAttempted(RoutingContext routingContext) {
        routingContext.put(ATTEMPT_AUTH_INVOKED, TRUE);
    }

    private static boolean areAuthMechanismsSelected(RoutingContext routingContext) {
        return getSelectedAuthMechanisms(routingContext) != null;
    }

    private static boolean authenticationAlreadyAttempted(RoutingContext event, String newAuthMechanism) {
        return event.get(ATTEMPT_AUTH_INVOKED) == TRUE
                && authenticationAttemptedWithDifferentRequirements(newAuthMechanism, event);
    }

    private static boolean authenticationAttemptedWithDifferentRequirements(String newAuthMechanism, RoutingContext event) {
        // this is configured used for the previous attempt, we require that this is identical to what is selected
        // by the annotation
        AuthenticationMechanisms selectedAuthenticationMechanisms = getSelectedAuthMechanisms(event);

        if (selectedAuthenticationMechanisms != null) {
            // for now, users can only select one mechanism with the annotation, hence, the previous
            // authentication attempt must be identical in every way
            if (selectedAuthenticationMechanisms.names().size() == 1
                    && selectedAuthenticationMechanisms.names().contains(normalizeMechanismName(newAuthMechanism))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Remember authentication mechanism used for authentication so that we know what mechanism has been used
     * in case that someone tries to change the mechanism after the authentication. This way, we can be permissive
     * when the selected mechanism is same as the one already used.
     */
    private static Uni<HttpCredentialTransport> rememberSelectedAuthMechScheme(HttpAuthenticationMechanism mech,
            RoutingContext event) {
        return mech.getCredentialTransport(event)
                .onItem().ifNotNull().invoke(new Consumer<HttpCredentialTransport>() {
                    @Override
                    public void accept(HttpCredentialTransport t) {
                        if (t.getAuthenticationScheme() != null) {
                            rememberSelectedAuthMechanism(event, t.getAuthenticationScheme());
                        }
                    }
                });
    }

    private static void rememberSelectedAuthMechanism(RoutingContext event, String newAuthMechanism) {
        AuthenticationMechanisms currentMechanisms = getSelectedAuthMechanisms(event);
        if (currentMechanisms == null) {
            event.put(SELECTED_AUTH_MECHANISMS, new AuthenticationMechanisms(newAuthMechanism));
        } else {
            event.put(SELECTED_AUTH_MECHANISMS, currentMechanisms.with(newAuthMechanism));
        }
    }

    private static AuthenticationMechanisms getSelectedAuthMechanisms(RoutingContext event) {
        return event.get(SELECTED_AUTH_MECHANISMS);
    }

    private static HttpAuthenticationMechanism[] getSelectedAuthMechanismInstances(RoutingContext event) {
        return getMechanismsFromContext(event, SELECTED_AUTH_MECHANISM_INSTANCES);
    }

    private static void addSelectedAuthMechanismInstance(HttpAuthenticationMechanism httpAuthenticationMechanism,
            RoutingContext routingContext) {
        addMechanismToRoutingContext(httpAuthenticationMechanism, routingContext, SELECTED_AUTH_MECHANISM_INSTANCES);
    }

    private static void addMechanismToRoutingContext(HttpAuthenticationMechanism httpAuthenticationMechanism,
            RoutingContext routingContext, String selectedAuthMechanismInstances) {
        List<HttpAuthenticationMechanism> mechanisms = routingContext.get(selectedAuthMechanismInstances);
        if (mechanisms == null) {
            mechanisms = new ArrayList<>();
            routingContext.put(selectedAuthMechanismInstances, mechanisms);
        }
        if (!mechanisms.contains(httpAuthenticationMechanism)) {
            mechanisms.add(httpAuthenticationMechanism);
        }
    }

    private static HttpAuthenticationMechanism[] getMechanismsFromContext(RoutingContext event, String mechanismsKey) {
        List<HttpAuthenticationMechanism> selectedInstances = event.get(mechanismsKey);
        if (selectedInstances == null || selectedInstances.isEmpty()) {
            return null;
        }
        return selectedInstances.toArray(HttpAuthenticationMechanism[]::new);
    }

    static final class NoAuthenticationMechanism implements HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context,
                IdentityProviderManager identityProviderManager) {
            return Uni.createFrom().optional(Optional.empty());
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            ChallengeData challengeData = new ChallengeData(HttpResponseStatus.FORBIDDEN.code());
            return Uni.createFrom().item(challengeData);
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return Collections.singleton(AnonymousAuthenticationRequest.class);
        }

    }

}
