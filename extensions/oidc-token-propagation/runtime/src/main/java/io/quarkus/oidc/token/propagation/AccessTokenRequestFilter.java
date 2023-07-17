package io.quarkus.oidc.token.propagation;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig.Grant;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.token.propagation.runtime.AbstractTokenRequestFilter;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.credential.TokenCredential;

public class AccessTokenRequestFilter extends AbstractTokenRequestFilter {
    // note: We can't use constructor injection for these fields because they are registered by RESTEasy
    // which doesn't know about CDI at the point of registration

    @Inject
    Instance<TokenCredential> accessToken;

    @Inject
    @ConfigProperty(name = "quarkus.oidc-token-propagation.client-name")
    Optional<String> oidcClientName;
    @Inject
    @ConfigProperty(name = "quarkus.oidc-token-propagation.exchange-token")
    boolean exchangeToken;

    OidcClient exchangeTokenClient;
    String exchangeTokenProperty;

    @PostConstruct
    public void initExchangeTokenClient() {
        if (exchangeToken) {
            OidcClients clients = Arc.container().instance(OidcClients.class).get();
            exchangeTokenClient = oidcClientName.isPresent() ? clients.getClient(oidcClientName.get()) : clients.getClient();
            Grant.Type exchangeTokenGrantType = ConfigProvider.getConfig()
                    .getValue(
                            "quarkus.oidc-client." + (oidcClientName.isPresent() ? oidcClientName.get() + "." : "")
                                    + "grant.type",
                            Grant.Type.class);
            if (exchangeTokenGrantType == Grant.Type.EXCHANGE) {
                exchangeTokenProperty = "subject_token";
            } else if (exchangeTokenGrantType == Grant.Type.JWT) {
                exchangeTokenProperty = "assertion";
            } else {
                throw new ConfigurationException("Token exchange is required but OIDC client is configured "
                        + "to use the " + exchangeTokenGrantType.getGrantType() + " grantType");
            }
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
            return exchangeTokenClient.getTokens(Collections.singletonMap(exchangeTokenProperty, token))
                    .await().indefinitely().getAccessToken();
        } else {
            return token;
        }
    }
}
