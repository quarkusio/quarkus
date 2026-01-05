package io.quarkus.oidc.client.graphql.runtime;

import java.util.Optional;

import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.runtime.AbstractTokensProducer;
import io.smallrye.mutiny.Uni;

/**
 * Abstract token provider parent extended by generated provider beans.
 */
public abstract class AbstractGraphQLTokenProvider extends AbstractTokensProducer {

    private final String oidcClientId;

    protected AbstractGraphQLTokenProvider(String oidcClientId) {
        this.oidcClientId = oidcClientId;
    }

    final Uni<String> getAccessToken() {
        return Uni.createFrom()
                .deferred(this::getTokens)
                .map(Tokens::getAccessToken)
                .map(token -> "Bearer " + token);
    }

    @Override
    protected Optional<String> clientId() {
        return Optional.of(oidcClientId);
    }
}
