package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
import io.vertx.ext.web.RoutingContext;

/**
 * Class that is responsible for running the HTTP based authentication
 */
@ApplicationScoped
public class HttpAuthenticator {

    @Inject
    IdentityProviderManager identityProviderManager;

    final HttpAuthenticationMechanism[] mechanisms;

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

    /**
     * Attempts authentication with the contents of the request. If this is possible the CompletionStage
     * will resolve to a valid SecurityIdentity.
     *
     * If invalid credentials are present then the completion stage will resolve to a
     * {@link io.quarkus.security.AuthenticationFailedException}
     *
     * If no credentials are present it will resolve to null.
     */
    public CompletionStage<SecurityIdentity> attemptAuthentication(RoutingContext routingContext) {

        CompletionStage<SecurityIdentity> result = mechanisms[0].authenticate(routingContext, identityProviderManager);
        for (int i = 1; i < mechanisms.length; ++i) {
            HttpAuthenticationMechanism mech = mechanisms[i];
            result = result.thenCompose(new Function<SecurityIdentity, CompletionStage<SecurityIdentity>>() {
                @Override
                public CompletionStage<SecurityIdentity> apply(SecurityIdentity data) {
                    if (data != null) {
                        return CompletableFuture.completedFuture(data);
                    }
                    return mech.authenticate(routingContext, identityProviderManager);
                }
            });
        }

        return result;
    }

    /**
     *
     * @param closeTask The task that should be run to finalize the HTTP exchange.
     * @return
     */
    public CompletionStage<Void> sendChallenge(RoutingContext routingContext, Runnable closeTask) {
        if (closeTask == null) {
            closeTask = NoopCloseTask.INSTANCE;
        }
        CompletionStage<Boolean> result = mechanisms[0].sendChallenge(routingContext);
        for (int i = 1; i < mechanisms.length; ++i) {
            HttpAuthenticationMechanism mech = mechanisms[i];
            result = result.thenCompose(new Function<Boolean, CompletionStage<Boolean>>() {
                @Override
                public CompletionStage<Boolean> apply(Boolean aBoolean) {
                    if (aBoolean) {
                        return CompletableFuture.completedFuture(true);
                    }
                    return mech.sendChallenge(routingContext);
                }
            });
        }
        return result.thenRun(closeTask);
    }

    public CompletionStage<ChallengeData> getChallenge(RoutingContext routingContext) {
        CompletionStage<ChallengeData> result = mechanisms[0].getChallenge(routingContext);
        for (int i = 1; i < mechanisms.length; ++i) {
            HttpAuthenticationMechanism mech = mechanisms[i];
            result = result.thenCompose(new Function<ChallengeData, CompletionStage<ChallengeData>>() {
                @Override
                public CompletionStage<ChallengeData> apply(ChallengeData data) {
                    if (data != null) {
                        return CompletableFuture.completedFuture(data);
                    }
                    return mech.getChallenge(routingContext);
                }
            });
        }
        return result;
    }

    static class NoAuthenticationMechanism implements HttpAuthenticationMechanism {

        @Override
        public CompletionStage<SecurityIdentity> authenticate(RoutingContext context,
                IdentityProviderManager identityProviderManager) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<ChallengeData> getChallenge(RoutingContext context) {
            ChallengeData challengeData = new ChallengeData(HttpResponseStatus.FORBIDDEN.code(), null, null);
            return CompletableFuture.completedFuture(challengeData);
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
