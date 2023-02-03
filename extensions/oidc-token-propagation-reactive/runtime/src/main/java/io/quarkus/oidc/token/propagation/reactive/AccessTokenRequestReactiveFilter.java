package io.quarkus.oidc.token.propagation.reactive;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig.Grant;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.DisabledOidcClientException;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.credential.TokenCredential;
import io.smallrye.mutiny.Uni;

@Priority(Priorities.AUTHENTICATION)
public class AccessTokenRequestReactiveFilter implements ResteasyReactiveClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(AccessTokenRequestReactiveFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = "Bearer ";

    @Inject
    Instance<TokenCredential> accessToken;

    @Inject
    @ConfigProperty(name = "quarkus.oidc-token-propagation-reactive.client-name")
    Optional<String> oidcClientName;
    @Inject
    @ConfigProperty(name = "quarkus.oidc-token-propagation-reactive.exchange-token")
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
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        if (verifyTokenInstance(requestContext)) {
            if (exchangeTokenClient != null) {

                requestContext.suspend();

                exchangeToken(accessToken.get().getToken()).subscribe().with(new Consumer<>() {
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
                propagateToken(requestContext, accessToken.get().getToken());
            }
        } else {
            abortRequest(requestContext);
        }
    }

    public void propagateToken(ResteasyReactiveClientRequestContext requestContext, String accessToken) {
        if (accessToken != null) {
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_WITH_SPACE + accessToken);
        } else {
            LOG.debugf("Access token is null, aborting the request with HTTP 401 error");
            abortRequest(requestContext);
        }
    }

    protected boolean verifyTokenInstance(ResteasyReactiveClientRequestContext requestContext) {
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

    private Uni<String> exchangeToken(String token) {
        return exchangeTokenClient.getTokens(Collections.singletonMap(exchangeTokenProperty, token))
                .onItem().transform(t -> t.getAccessToken());

    }

    protected void abortRequest(ResteasyReactiveClientRequestContext requestContext) {
        requestContext.abortWith(Response.status(401).build());
    }
}
