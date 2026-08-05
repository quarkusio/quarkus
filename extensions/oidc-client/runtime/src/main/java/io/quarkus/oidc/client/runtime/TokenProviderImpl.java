package io.quarkus.oidc.client.runtime;

import java.util.Map;

import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.spi.TokenProvider;
import io.smallrye.mutiny.Uni;

public record TokenProviderImpl(AbstractTokensProducer tokensProducer) implements TokenProvider {
    @Override
    public Uni<String> getAccessToken() {
        return tokensProducer.getTokens().onItem().transform(Tokens::getAccessToken);
    }

    @Override
    public Uni<String> getAccessToken(Map<String, String> additionalParameters) {
        return tokensProducer.getTokens(additionalParameters).onItem().transform(Tokens::getAccessToken);
    }
}
