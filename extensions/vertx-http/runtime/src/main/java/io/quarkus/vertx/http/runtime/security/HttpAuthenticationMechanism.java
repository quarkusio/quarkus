package io.quarkus.vertx.http.runtime.security;

import java.util.Set;
import java.util.function.Function;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * An interface that performs HTTP based authentication
 */
public interface HttpAuthenticationMechanism {

    int DEFAULT_PRIORITY = 1000;

    Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager);

    Uni<ChallengeData> getChallenge(RoutingContext context);

    /**
     * Returns the required credential types. If there are no identity managers installed that support the
     * listed types then this mechanism will not be enabled.
     */
    Set<Class<? extends AuthenticationRequest>> getCredentialTypes();

    default Uni<Boolean> sendChallenge(RoutingContext context) {
        return getChallenge(context).map(new ChallengeSender(context));
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

    /**
     * Returns a priority which determines in which order HttpAuthenticationMechanisms handle the authentication and challenge
     * requests
     * when it is not possible to select the best candidate authentication mechanism based on the request credentials or path
     * specific
     * configuration.
     *
     * Multiple mechanisms are sorted in descending order, so highest priority gets the first chance to send a challenge. The
     * default priority is equal to 1000.
     *
     * @return priority
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }
}
