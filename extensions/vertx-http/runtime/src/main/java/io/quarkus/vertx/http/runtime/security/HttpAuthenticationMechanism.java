package io.quarkus.vertx.http.runtime.security;

import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * An interface that performs HTTP based authentication
 */
public interface HttpAuthenticationMechanism {

    CompletionStage<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager);

    CompletionStage<ChallengeData> getChallenge(RoutingContext context);

    /**
     * Returns the required credential types. If there are no identity managers installed that support the
     * listed types then this mechanism will not be enabled.
     */
    Set<Class<? extends AuthenticationRequest>> getCredentialTypes();

    default CompletionStage<Boolean> sendChallenge(RoutingContext context) {
        return getChallenge(context).thenApply(new ChallengeSender(context));
    }

    /**
     * The credential transport, used to make sure multiple incompatible mechanisms are not installed
     * 
     * May be null if this mechanism cannot interfere with other mechanisms
     */
    HttpCredentialTransport getCredentialTransport();

    class ChallengeSender implements Function<ChallengeData, Boolean> {

        private final RoutingContext context;

        public ChallengeSender(RoutingContext context) {
            this.context = context;
        }

        @Override
        public Boolean apply(ChallengeData challengeData) {
            if (challengeData == null) {
                return false;
            }
            context.response().setStatusCode(challengeData.status);
            if (challengeData.headerName != null) {
                context.response().headers().set(challengeData.headerName, challengeData.headerContent);
            }
            return true;
        }
    }
}
