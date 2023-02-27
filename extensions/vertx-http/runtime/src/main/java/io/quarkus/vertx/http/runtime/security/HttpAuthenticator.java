package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Class that is responsible for running the HTTP based authentication
 */
@Singleton
public class HttpAuthenticator {
    private static final Logger log = Logger.getLogger(HttpAuthenticator.class);

    private final IdentityProviderManager identityProviderManager;
    private final Instance<PathMatchingHttpSecurityPolicy> pathMatchingPolicy;
    private final HttpAuthenticationMechanism[] mechanisms;

    public HttpAuthenticator(IdentityProviderManager identityProviderManager,
            Instance<PathMatchingHttpSecurityPolicy> pathMatchingPolicy,
            Instance<HttpAuthenticationMechanism> httpAuthenticationMechanism,
            Instance<IdentityProvider<?>> providers) {
        this.identityProviderManager = identityProviderManager;
        this.pathMatchingPolicy = pathMatchingPolicy;
        List<HttpAuthenticationMechanism> mechanisms = new ArrayList<>();
        for (HttpAuthenticationMechanism mechanism : httpAuthenticationMechanism) {
            boolean found = false;
            for (Class<? extends AuthenticationRequest> mechType : mechanism.getCredentialTypes()) {
                for (IdentityProvider<?> i : providers) {
                    if (i.getRequestType().equals(mechType)) {
                        found = true;
                        break;
                    }
                }
                if (found == true) {
                    break;
                }
            }
            // Add mechanism if there is a provider with matching credential type
            // If the mechanism has no credential types, just add it anyway
            if (found || mechanism.getCredentialTypes().isEmpty()) {
                mechanisms.add(mechanism);
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

    IdentityProviderManager getIdentityProviderManager() {
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
    public Uni<SecurityIdentity> attemptAuthentication(RoutingContext routingContext, boolean inclusiveAuthentication) {

        String pathSpecificMechanism = pathMatchingPolicy.isResolvable()
                ? pathMatchingPolicy.get().getAuthMechanismName(routingContext)
                : null;
        Uni<HttpAuthenticationMechanism> matchingMechUni = inclusiveAuthentication ? null
                : findBestCandidateMechanism(routingContext, pathSpecificMechanism);
        if (matchingMechUni == null) {
            return createSecurityIdentity(routingContext, inclusiveAuthentication);
        }

        return matchingMechUni.onItem()
                .transformToUni(new Function<HttpAuthenticationMechanism, Uni<? extends SecurityIdentity>>() {

                    @Override
                    public Uni<SecurityIdentity> apply(HttpAuthenticationMechanism mech) {
                        if (mech != null) {
                            return mech.authenticate(routingContext, identityProviderManager);
                        } else if (pathSpecificMechanism != null) {
                            return Uni.createFrom().optional(Optional.empty());
                        }
                        return createSecurityIdentity(routingContext, false);
                    }

                });

    }

    private Uni<SecurityIdentity> createSecurityIdentity(RoutingContext routingContext, boolean inclusiveAuthentication) {
        Uni<SecurityIdentity> result = mechanisms[0].authenticate(routingContext, identityProviderManager);

        if (!inclusiveAuthentication) {
            // Return the first SecurityIdentity
            for (int i = 1; i < mechanisms.length; ++i) {
                HttpAuthenticationMechanism mech = mechanisms[i];
                result = result.onItem().transformToUni(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(SecurityIdentity identity) {
                        if (identity != null) {
                            return Uni.createFrom().item(identity);
                        }
                        return mech.authenticate(routingContext, identityProviderManager);
                    }
                });
            }
            return result;
        } else {
            // Have all authentication mechanisms produce SecurityIdentity
            for (int i = 1; i < mechanisms.length; ++i) {
                final int currentIndex = i;
                HttpAuthenticationMechanism mech = mechanisms[i];
                result = result.onItem().transformToUni(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(SecurityIdentity identity) {
                        if (identity != null) {
                            SecurityIdentity firstIdentity = routingContext.get(HttpSecurityUtils.SECURITY_IDENTITY_ATTRIBUTE);
                            if (firstIdentity == null) {
                                // If it is the very first SecurityIdentity then save it as a 'io.quarkus.security.identity' attribute on RoutingContext.
                                firstIdentity = identity;
                                routingContext.put(HttpSecurityUtils.SECURITY_IDENTITY_ATTRIBUTE, firstIdentity);
                                // Get the second authentication mechanism produce SecurityIdentity
                                return mech.authenticate(routingContext, identityProviderManager);
                            } else {
                                // Otherwise add it to the first SecurityIdentity's 'io.quarkus.security.identities' map attribute.
                                // This branch will run if there are at least 3 authentication mechanism
                                final Map<String, SecurityIdentity> identities = getSecurityIdentities(firstIdentity);
                                return getCredentialTransport(mechanisms[currentIndex - 1], routingContext)
                                        .onItem().transformToUni(
                                                new Function<HttpCredentialTransport, Uni<? extends SecurityIdentity>>() {
                                                    @Override
                                                    public Uni<SecurityIdentity> apply(HttpCredentialTransport transport) {
                                                        // Save the extra identity in the identities map
                                                        // Get the next authentication mechanism produce SecurityIdentity
                                                        identities.put(transport.getAuthenticationScheme(), identity);
                                                        return mech.authenticate(routingContext, identityProviderManager);
                                                    }
                                                });
                            }

                        }
                        return Uni.createFrom().optional(Optional.empty());
                    }
                });
            }

            // Now make sure the first SecurityIdentity is returned since the current SecurityIdentity was created
            // by the last authentication mechanism in the list
            return result.onItem().transformToUni(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                @Override
                public Uni<SecurityIdentity> apply(final SecurityIdentity identity) {
                    if (identity != null) {
                        final SecurityIdentity firstIdentity = routingContext
                                .get(HttpSecurityUtils.SECURITY_IDENTITY_ATTRIBUTE);
                        if (firstIdentity != null) {
                            final Map<String, SecurityIdentity> identities = getSecurityIdentities(firstIdentity);

                            return getCredentialTransport(mechanisms[mechanisms.length - 1], routingContext)
                                    .onItem().transform(new Function<HttpCredentialTransport, SecurityIdentity>() {
                                        @Override
                                        public SecurityIdentity apply(HttpCredentialTransport transport) {
                                            // Save the extra identity in the identities map
                                            identities.put(transport.getAuthenticationScheme(), identity);
                                            // Return the first identity
                                            return firstIdentity;
                                        }
                                    });
                        } else {
                            // Single authentication mechanism is available only
                            return Uni.createFrom().item(identity);
                        }
                    }
                    return Uni.createFrom().optional(Optional.empty());
                }
            });
        }
    }

    private Map<String, SecurityIdentity> getSecurityIdentities(SecurityIdentity firstIdentity) {
        Map<String, SecurityIdentity> identities = firstIdentity.getAttribute(HttpSecurityUtils.SECURITY_IDENTITIES_ATTRIBUTE);
        if (identities == null) {
            identities = new LinkedHashMap<>();
            firstIdentity.getAttributes().put(HttpSecurityUtils.SECURITY_IDENTITIES_ATTRIBUTE, identities);
        }
        return identities;
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

    static class NoopCloseTask implements Runnable {

        static final NoopCloseTask INSTANCE = new NoopCloseTask();

        @Override
        public void run() {

        }
    }

}
