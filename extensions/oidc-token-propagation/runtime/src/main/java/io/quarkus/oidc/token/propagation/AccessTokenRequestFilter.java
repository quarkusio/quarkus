package io.quarkus.oidc.token.propagation;

import static io.quarkus.oidc.token.propagation.common.runtime.TokenPropagationConstants.JWT_PROPAGATE_TOKEN_CREDENTIAL;
import static io.quarkus.oidc.token.propagation.common.runtime.TokenPropagationConstants.OIDC_PROPAGATE_TOKEN_CREDENTIAL;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.client.ClientRequestContext;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.token.propagation.runtime.AbstractTokenRequestFilter;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.vertx.core.Vertx;

public class AccessTokenRequestFilter extends AbstractTokenRequestFilter {
    // note: We can't use constructor injection for these fields because they are registered by RESTEasy
    // which doesn't know about CDI at the point of registration

    private static final String INVOKED_METHOD_PROP = "org.eclipse.microprofile.rest.client.invokedMethod";
    private static final String ERROR_MSG = "OIDC Token Propagation requires a safe (isolated) Vert.x sub-context because configuration property 'quarkus.resteasy-client-oidc-token-propagation.enabled-during-authentication' has been set to true, but the current context hasn't been flagged as such.";
    private final boolean enabledDuringAuthentication;
    private final Instance<TokenCredential> accessToken;

    OidcClient exchangeTokenClient;
    String exchangeTokenProperty;

    public AccessTokenRequestFilter() {
        this.enabledDuringAuthentication = Boolean.getBoolean(OIDC_PROPAGATE_TOKEN_CREDENTIAL)
                || Boolean.getBoolean(JWT_PROPAGATE_TOKEN_CREDENTIAL);
        this.accessToken = CDI.current().select(TokenCredential.class);
    }

    @PostConstruct
    public void initExchangeTokenClient() {
        if (isExchangeToken()) {
            OidcClients clients = Arc.container().instance(OidcClients.class).get();
            String clientName = getClientName();
            exchangeTokenClient = clientName != null ? clients.getClient(clientName) : clients.getClient();
            Grant.Type exchangeTokenGrantType = ConfigProvider.getConfig()
                    .getValue(
                            "quarkus.oidc-client." + (clientName != null ? clientName + "." : "")
                                    + "grant.type",
                            Grant.Type.class);
            if (exchangeTokenGrantType == Grant.Type.EXCHANGE) {
                exchangeTokenProperty = OidcConstants.EXCHANGE_GRANT_SUBJECT_TOKEN;
            } else if (exchangeTokenGrantType == Grant.Type.JWT) {
                exchangeTokenProperty = OidcConstants.JWT_BEARER_GRANT_ASSERTION;
            } else {
                throw new ConfigurationException("Token exchange is required but OIDC client is configured "
                        + "to use the " + exchangeTokenGrantType.getGrantType() + " grantType");
            }
        }
    }

    protected boolean isExchangeToken() {
        return ConfigProvider.getConfig().getValue("quarkus.resteasy-client-oidc-token-propagation.exchange-token",
                boolean.class);
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (skipPropagation(requestContext)) {
            return;
        }

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
        return ConfigProvider.getConfig()
                .getOptionalValue("quarkus.resteasy-client-oidc-token-propagation.client-name", String.class)
                .orElse(null);
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

    private boolean skipPropagation(ClientRequestContext requestContext) {
        if (getMethodDescription() == null) {
            return false;
        }

        return !getMethodDescription().equals(getInvokedRestClientMethodSignature(requestContext));
    }

    private static MethodDescription getInvokedRestClientMethodSignature(ClientRequestContext requestContext) {
        Method method = (Method) requestContext.getProperty(INVOKED_METHOD_PROP);
        if (method == null) {
            throw new IllegalStateException(INVOKED_METHOD_PROP + " property must not be null");
        }
        return MethodDescription.ofMethod(method);
    }

    /**
     * This method is overridden by generated filter classes if the filter should only be applied on the REST client
     * method.
     *
     * @return REST client method description for which this filter should be applied; or null if applies to all methods
     */
    protected MethodDescription getMethodDescription() {
        return null;
    }
}
