package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

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
@ApplicationScoped
public class HttpAuthenticator {

    final HttpAuthenticationMechanism[] mechanisms;
    @Inject
    IdentityProviderManager identityProviderManager;

    public HttpAuthenticator() {
        mechanisms = null;
    }

    @Inject
    public HttpAuthenticator(Instance<HttpAuthenticationMechanism> instance,
            Instance<IdentityProvider<?>> providers) {
        List<HttpAuthenticationMechanism> mechanisms = new ArrayList<>();
        for (HttpAuthenticationMechanism mechanism : instance) {
            boolean notFound = false;
            for (Class<? extends AuthenticationRequest> mechType : mechanism.getCredentialTypes()) {
                boolean found = false;
                for (IdentityProvider<?> i : providers) {
                    if (i.getRequestType().equals(mechType)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    notFound = true;
                    break;
                }
            }
            if (!notFound) {
                mechanisms.add(mechanism);
            }
        }
        if (mechanisms.isEmpty()) {
            this.mechanisms = new HttpAuthenticationMechanism[] { new NoAuthenticationMechanism() };
        } else {
            this.mechanisms = mechanisms.toArray(new HttpAuthenticationMechanism[mechanisms.size()]);
            //validate that we don't have multiple incompatible mechanisms
            Map<HttpCredentialTransport, HttpAuthenticationMechanism> map = new HashMap<>();
            for (HttpAuthenticationMechanism i : mechanisms) {
                HttpCredentialTransport credentialTransport = i.getCredentialTransport();
                if (credentialTransport == null) {
                    continue;
                }
                HttpAuthenticationMechanism existing = map.get(credentialTransport);
                if (existing != null) {
                    throw new RuntimeException("Multiple mechanisms present that use the same credential transport "
                            + credentialTransport + ". Mechanisms are " + i + " and " + existing);
                }
                map.put(credentialTransport, i);
            }

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
    public Uni<SecurityIdentity> attemptAuthentication(RoutingContext routingContext) {

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
        Uni<Boolean> result = mechanisms[0].sendChallenge(routingContext);
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
        return result.onItem().transformToUni(new Function<Boolean, Uni<? extends Boolean>>() {
            @Override
            public Uni<? extends Boolean> apply(Boolean authDone) {
                if (!authDone) {
                    routingContext.response().setStatusCode(401);
                    routingContext.response().end();
                }
                return Uni.createFrom().item(authDone);
            }
        });
    }

    public Uni<ChallengeData> getChallenge(RoutingContext routingContext) {
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
