package io.quarkus.oidc.client.runtime;

import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.spi.TokenProvider;
import io.smallrye.mutiny.Uni;

public record TokenProviderImpl(AbstractTokensProducer tokensProducer) implements TokenProvider {
    @Override
    public Uni<String> getAccessToken() {
        return tokensProducer.getTokens().onItem().transform(Tokens::getAccessToken);
    }
}
