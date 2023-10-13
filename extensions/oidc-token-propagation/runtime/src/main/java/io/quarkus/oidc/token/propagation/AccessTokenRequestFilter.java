package io.quarkus.oidc.token.propagation;

import static io.quarkus.oidc.token.propagation.TokenPropagationConstants.JWT_PROPAGATE_TOKEN_CREDENTIAL;
import static io.quarkus.oidc.token.propagation.TokenPropagationConstants.OIDC_PROPAGATE_TOKEN_CREDENTIAL;

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
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.vertx.core.Vertx;

public class AccessTokenRequestFilter extends AbstractTokenRequestFilter {
    // note: We can't use constructor injection for these fields because they are registered by RESTEasy
    // which doesn't know about CDI at the point of registration

    private static final String ERROR_MSG = "OIDC Token Propagation requires a safe (isolated) Vert.x sub-context because configuration property 'quarkus.oidc-token-propagation.enabled-during-authentication' has been set to true, but the current context hasn't been flagged as such.";
    private final boolean enabledDuringAuthentication;

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

    public AccessTokenRequestFilter() {
        this.enabledDuringAuthentication = Boolean.getBoolean(OIDC_PROPAGATE_TOKEN_CREDENTIAL)
                || Boolean.getBoolean(JWT_PROPAGATE_TOKEN_CREDENTIAL);
    }

    @PostConstruct
    public void initExchangeTokenClient() {
        if (exchangeToken) {
            OidcClients clients = Arc.container().instance(OidcClients.class).get();
            String clientName = getClientName();
            exchangeTokenClient = clientName != null ? clients.getClient(clientName) : clients.getClient();
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
        if (acquireTokenCredentialFromCtx(requestContext)) {
            propagateToken(requestContext, exchangeTokenIfNeeded(getTokenCredentialFromContext().getToken()));
        } else {
            if (verifyTokenInstance(requestContext, accessToken)) {
                propagateToken(requestContext, exchangeTokenIfNeeded(accessToken.get().getToken()));
            }
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

    protected String getClientName() {
        return oidcClientName.orElse(null);
    }

    private boolean acquireTokenCredentialFromCtx(ClientRequestContext requestContext) {
        if (enabledDuringAuthentication) {
            TokenCredential tokenCredential = getTokenCredentialFromContext();
            if (tokenCredential != null) {
                if (tokenCredential.getToken() == null) {
                    abortRequest(requestContext);
                } else {
                    return true;
                }
            }
            // this means auth is already done, and we need to use CDI
        }
        return false;
    }

    private static TokenCredential getTokenCredentialFromContext() {
        VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG, ERROR_MSG);
        return Vertx.currentContext().getLocal(TokenCredential.class.getName());
    }
}
