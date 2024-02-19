package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHENTICATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHENTICATION_SUCCESS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Class that is responsible for running the HTTP based authentication
 */
@Singleton
public class HttpAuthenticator {
    private static final Logger log = Logger.getLogger(HttpAuthenticator.class);

    private final IdentityProviderManager identityProviderManager;
    private final HttpAuthenticationMechanism[] mechanisms;
    private final SecurityEventHelper<AuthenticationSuccessEvent, AuthenticationFailureEvent> securityEventHelper;

    public HttpAuthenticator(IdentityProviderManager identityProviderManager,
            Event<AuthenticationFailureEvent> authFailureEvent,
            Event<AuthenticationSuccessEvent> authSuccessEvent,
            BeanManager beanManager, HttpBuildTimeConfig httpBuildTimeConfig,
            Instance<HttpAuthenticationMechanism> httpAuthenticationMechanism,
            Instance<IdentityProvider<?>> providers,
            @ConfigProperty(name = "quarkus.security.events.enabled") boolean securityEventsEnabled) {
        this.securityEventHelper = new SecurityEventHelper<>(authSuccessEvent, authFailureEvent, AUTHENTICATION_SUCCESS,
                AUTHENTICATION_FAILURE, beanManager, securityEventsEnabled);
        this.identityProviderManager = identityProviderManager;
        List<HttpAuthenticationMechanism> mechanisms = new ArrayList<>();
        for (HttpAuthenticationMechanism mechanism : httpAuthenticationMechanism) {
            if (mechanism.getCredentialTypes().isEmpty()) {
                // mechanism does not require any IdentityProvider
                log.debugf("HttpAuthenticationMechanism '%s' provided no required credential types, therefore it needs "
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
                    && httpBuildTimeConfig.auth.basic.isEmpty()) {
                log.debug("""
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
        AbstractPathMatchingHttpSecurityPolicy pathMatchingPolicy = routingContext
                .get(AbstractPathMatchingHttpSecurityPolicy.class.getName());

        String pathSpecificMechanism = pathMatchingPolicy != null
                ? pathMatchingPolicy.getAuthMechanismName(routingContext)
                : null;
        Uni<HttpAuthenticationMechanism> matchingMechUni = findBestCandidateMechanism(routingContext, pathSpecificMechanism);
        Uni<SecurityIdentity> result;
        if (matchingMechUni == null) {
            result = createSecurityIdentity(routingContext);
        } else {
            result = matchingMechUni.onItem()
                    .transformToUni(new Function<HttpAuthenticationMechanism, Uni<? extends SecurityIdentity>>() {

                        @Override
                        public Uni<SecurityIdentity> apply(HttpAuthenticationMechanism mech) {
                            if (mech != null) {
                                return mech.authenticate(routingContext, identityProviderManager);
                            } else if (pathSpecificMechanism != null) {
                                return Uni.createFrom().optional(Optional.empty());
                            }
                            return createSecurityIdentity(routingContext);
                        }

                    });
        }
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

    private Uni<SecurityIdentity> createSecurityIdentity(RoutingContext routingContext) {
        Uni<SecurityIdentity> result = mechanisms[0].authenticate(routingContext, identityProviderManager);
        for (int i = 1; i < mechanisms.length; ++i) {
            HttpAuthenticationMechanism mech = mechanisms[i];
            result = result.onItem().transformToUni(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                @Override
                public Uni<SecurityIdentity> apply(SecurityIdentity data) {
                    if (data != null) {
                        return Uni.createFrom().item(data);
                    }
                    return mech.authenticate(routingContext, identityProviderManager);
                }
            });
        }
        return result;
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
                    log.debug("Authentication has not been done, returning HTTP status 401");
                    routingContext.response().setStatusCode(401);
                    routingContext.response().end();
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

    private Uni<HttpAuthenticationMechanism> findBestCandidateMechanism(RoutingContext routingContext,
            String pathSpecificMechanism) {
        Uni<HttpAuthenticationMechanism> result = null;

        if (pathSpecificMechanism != null) {
            result = getPathSpecificMechanism(0, routingContext, pathSpecificMechanism);
            for (int i = 1; i < mechanisms.length; ++i) {
                int mechIndex = i;
                result = result.onItem().transformToUni(
                        new Function<HttpAuthenticationMechanism, Uni<? extends HttpAuthenticationMechanism>>() {
                            @Override
                            public Uni<? extends HttpAuthenticationMechanism> apply(HttpAuthenticationMechanism mech) {
                                if (mech != null) {
                                    return Uni.createFrom().item(mech);
                                }
                                return getPathSpecificMechanism(mechIndex, routingContext, pathSpecificMechanism);
                            }
                        });
            }
        }
        return result;
    }

    private Uni<HttpAuthenticationMechanism> getPathSpecificMechanism(int index, RoutingContext routingContext,
            String pathSpecificMechanism) {
        return getCredentialTransport(mechanisms[index], routingContext).onItem()
                .transform(new Function<HttpCredentialTransport, HttpAuthenticationMechanism>() {
                    @Override
                    public HttpAuthenticationMechanism apply(HttpCredentialTransport t) {
                        if (t != null && t.getAuthenticationScheme().equalsIgnoreCase(pathSpecificMechanism)) {
                            routingContext.put(HttpAuthenticationMechanism.class.getName(), mechanisms[index]);
                            return mechanisms[index];
                        }
                        return null;
                    }
                });
    }

    private static Uni<HttpCredentialTransport> getCredentialTransport(HttpAuthenticationMechanism mechanism,
            RoutingContext routingContext) {
        try {
            return mechanism.getCredentialTransport(routingContext);
        } catch (UnsupportedOperationException ex) {
            return Uni.createFrom().item(mechanism.getCredentialTransport());
        }
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

        @Override
        public HttpCredentialTransport getCredentialTransport() {
            return null;
        }

    }

}
