package io.quarkus.oidc.client.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.oidc.client.Tokens;

@Singleton
public class TokensProducer extends AbstractTokensProducer {

    @Produces
    @RequestScoped
    public Tokens produceTokens() {
        return awaitTokens();
    }
}
