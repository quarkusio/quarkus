package io.quarkus.oidc.client.runtime;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;

public abstract class AbstractTokensProducer {

    @Inject
    OidcClient oidcClient;

    @Inject
    @ConfigProperty(name = "quarkus.oidc-client.early-tokens-acquisition")
    boolean earlyTokenAcquisition;

    TokensHelper tokensHelper = new TokensHelper();

    @PostConstruct
    public void initTokens() {
        if (earlyTokenAcquisition) {
            tokensHelper.initTokens(oidcClient);
        }
    }

    public Uni<Tokens> getTokens() {
        return tokensHelper.getTokens(oidcClient);
    }
}
