package io.quarkus.oidc.client.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.oidc.client.spi.TokenProvider;
import io.smallrye.mutiny.Uni;

@Singleton
public class TokenProviderProducer extends AbstractTokensProducer {

    @Produces
    @RequestScoped
    public TokenProvider produceTokenProvider() {
        return new TokenProviderImpl();
    }

    class TokenProviderImpl implements TokenProvider {

        @Override
        public Uni<String> getAccessToken() {
            return TokenProviderProducer.super.getTokens().onItem().transform(t -> t.getAccessToken());
        }

    };
}
