package io.quarkus.vertx.http.runtime.security;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

/**
 * An interface that performs HTTP based authentication
 */
public interface HttpAuthenticationMechanism {

    CompletionStage<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager);

    CompletionStage<ChallengeData> getChallenge(RoutingContext context);

    default CompletionStage<Boolean> sendChallenge(RoutingContext context) {
        return getChallenge(context).thenApply(new ChallengeSender(context));
    }

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
