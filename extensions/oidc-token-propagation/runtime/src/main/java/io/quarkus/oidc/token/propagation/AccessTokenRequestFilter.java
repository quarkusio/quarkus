package io.quarkus.oidc.token.propagation;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.token.propagation.runtime.AbstractTokenRequestFilter;
import io.quarkus.security.credential.TokenCredential;

public class AccessTokenRequestFilter extends AbstractTokenRequestFilter {
    private static final String EXCHANGE_SUBJECT_TOKEN = "subject_token";

    // note: We can't use constructor injection for these fields because they are registered by RESTEasy
    // which doesn't know about CDI at the point of registration

    @Inject
    Instance<TokenCredential> accessToken;

    @Inject
    @ConfigProperty(name = "quarkus.oidc-token-propagation.client-name")
    Optional<String> clientName;
    @Inject
    @ConfigProperty(name = "quarkus.oidc-token-propagation.exchange-token")
    boolean exchangeToken;

    OidcClient exchangeTokenClient;

    @PostConstruct
    public void initExchangeTokenClient() {
        if (exchangeToken) {
            OidcClients clients = Arc.container().instance(OidcClients.class).get();
            exchangeTokenClient = clientName.isPresent() ? clients.getClient(clientName.get()) : clients.getClient();
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (verifyTokenInstance(requestContext, accessToken)) {
            propagateToken(requestContext, exchangeTokenIfNeeded(accessToken.get().getToken()));
        }
    }

    private String exchangeTokenIfNeeded(String token) {
        if (exchangeTokenClient != null) {
            // more dynamic parameters can be configured if required
            return exchangeTokenClient.getTokens(Collections.singletonMap(EXCHANGE_SUBJECT_TOKEN, token))
                    .await().indefinitely().getAccessToken();
        } else {
            return token;
        }
    }
}
