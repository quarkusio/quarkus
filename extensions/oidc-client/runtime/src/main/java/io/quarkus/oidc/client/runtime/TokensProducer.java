package io.quarkus.oidc.client.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.oidc.client.Tokens;

@Singleton
public class TokensProducer extends AbstractTokensProducer {

    @Produces
    @RequestScoped
    public Tokens produceTokens() {
        return awaitTokens();
    }
}
