package io.quarkus.oidc.token.propagation.reactive;

import static io.quarkus.oidc.token.propagation.common.runtime.TokenPropagationConstants.JWT_PROPAGATE_TOKEN_CREDENTIAL;
import static io.quarkus.oidc.token.propagation.common.runtime.TokenPropagationConstants.OIDC_PROPAGATE_TOKEN_CREDENTIAL;

import java.util.Collections;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.DisabledOidcClientException;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

@Priority(Priorities.AUTHENTICATION)
public class AccessTokenRequestReactiveFilter implements ResteasyReactiveClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(AccessTokenRequestReactiveFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = "Bearer ";
    private static final String ERROR_MSG = "OIDC Token Propagation Reactive requires a safe (isolated) Vert.x sub-context because configuration property 'quarkus.rest-client-oidc-token-propagation.enabled-during-authentication' has been set to true, but the current context hasn't been flagged as such.";
    private final boolean enabledDuringAuthentication;
    private final Instance<TokenCredential> accessToken;

    OidcClient exchangeTokenClient;
    String exchangeTokenProperty;

    public AccessTokenRequestReactiveFilter() {
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
                exchangeTokenProperty = "subject_token";
            } else if (exchangeTokenGrantType == Grant.Type.JWT) {
                exchangeTokenProperty = "assertion";
            } else {
                throw new ConfigurationException("Token exchange is required but OIDC client is configured "
                        + "to use the " + exchangeTokenGrantType.getGrantType() + " grantType");
            }
        }
    }

    protected boolean isExchangeToken() {
        return ConfigProvider.getConfig()
                .getValue("quarkus.rest-client-oidc-token-propagation.exchange-token", boolean.class);
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        if (verifyTokenInstance(requestContext)) {
            if (exchangeTokenClient != null) {

                requestContext.suspend();

                exchangeToken(getAccessToken().getToken()).subscribe().with(new Consumer<>() {
                    @Override
                    public void accept(String token) {
                        propagateToken(requestContext, token);
                        requestContext.resume();
                    }
                }, new Consumer<>() {
                    @Override
                    public void accept(Throwable t) {
                        if (t instanceof DisabledOidcClientException) {
                            LOG.debug("Client is disabled");
                            requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
                        } else {
                            LOG.debugf("Access token is not available, aborting the request with HTTP 401 error: %s",
                                    t.getMessage());
                            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                        }
                        requestContext.resume();
                    }
                });
            } else {
                propagateToken(requestContext, getAccessToken().getToken());
            }
        } else {
            abortRequest(requestContext);
        }
    }

    protected String getClientName() {
        return ConfigProvider
                .getConfig()
                .getOptionalValue("quarkus.rest-client-oidc-token-propagation.client-name", String.class)
                .orElse(null);
    }

    public void propagateToken(ResteasyReactiveClientRequestContext requestContext, String accessToken) {
        if (accessToken != null) {
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_WITH_SPACE + accessToken);
        } else {
            LOG.debugf("Access token is null, aborting the request with HTTP 401 error");
            abortRequest(requestContext);
        }
    }

    protected boolean verifyTokenInstance(ResteasyReactiveClientRequestContext requestContext) {
        if (enabledDuringAuthentication) {
            // TokenCredential cannot be accessed from CDI during authentication process
            TokenCredential tokenCredential = getTokenCredentialFromContext();
            if (tokenCredential != null) {
                if (tokenCredential.getToken() == null) {
                    LOG.debugf("Propagated access token is null, aborting the request with HTTP 401 error");
                    return false;
                }
                // use propagated access token
                return true;
            }
            // this means authentication is already done, therefore we use CDI
        }
        if (!accessToken.isResolvable()) {
            LOG.debugf("Access token is not injected, aborting the request with HTTP 401 error");
            return false;
        }
        if (accessToken.isAmbiguous()) {
            LOG.debugf("More than one access token instance is available, aborting the request with HTTP 401 error");
            return false;
        }
        if (accessToken.get().getToken() == null) {
            LOG.debugf("Injected access token is null, aborting the request with HTTP 401 error");
            return false;
        }
        return true;
    }

    private TokenCredential getAccessToken() {
        if (enabledDuringAuthentication) {
            TokenCredential tokenCredential = getTokenCredentialFromContext();
            if (tokenCredential != null) {
                return tokenCredential;
            }
            // this means auth is already done, therefore let's use CDI
        }
        return accessToken.get();
    }

    private static TokenCredential getTokenCredentialFromContext() {
        VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG, ERROR_MSG);
        return Vertx.currentContext().getLocal(TokenCredential.class.getName());
    }

    private Uni<String> exchangeToken(String token) {
        return exchangeTokenClient.getTokens(Collections.singletonMap(exchangeTokenProperty, token))
                .onItem().transform(t -> t.getAccessToken());

    }

    protected void abortRequest(ResteasyReactiveClientRequestContext requestContext) {
        requestContext.abortWith(Response.status(401).build());
    }
}
