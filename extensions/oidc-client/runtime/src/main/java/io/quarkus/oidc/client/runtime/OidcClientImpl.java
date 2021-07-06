package io.quarkus.oidc.client.runtime;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcClientImpl implements OidcClient {

    private static final Logger LOG = Logger.getLogger(OidcClientImpl.class);

    private static final String AUTHORIZATION_HEADER = String.valueOf(HttpHeaders.AUTHORIZATION);

    private final WebClient client;
    private final String tokenRequestUri;
    private final MultiMap tokenGrantParams;
    private final MultiMap commonRefreshGrantParams;
    private final String grantType;
    private final String clientSecretBasicAuthScheme;
    private final Key clientJwtKey;
    private final OidcClientConfig oidcConfig;
    private volatile boolean closed;

    public OidcClientImpl(WebClient client, String tokenRequestUri, String grantType,
            MultiMap tokenGrantParams, MultiMap commonRefreshGrantParams, OidcClientConfig oidcClientConfig) {
        this.client = client;
        this.tokenRequestUri = tokenRequestUri;
        this.tokenGrantParams = tokenGrantParams;
        this.commonRefreshGrantParams = commonRefreshGrantParams;
        this.grantType = grantType;
        this.oidcConfig = oidcClientConfig;
        this.clientSecretBasicAuthScheme = OidcCommonUtils.initClientSecretBasicAuth(oidcClientConfig);
        this.clientJwtKey = OidcCommonUtils.initClientJwtKey(oidcClientConfig);
    }

    @Override
    public Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters) {
        checkClosed();
        if (tokenGrantParams == null) {
            throw new OidcClientException(
                    "Only 'refresh_token' grant is supported, please call OidcClient#refreshTokens method instead");
        }
        return getJsonResponse(tokenGrantParams, additionalGrantParameters, false);
    }

    @Override
    public Uni<Tokens> refreshTokens(String refreshToken) {
        checkClosed();
        if (refreshToken == null) {
            throw new OidcClientException("Refresh token is null");
        }
        MultiMap refreshGrantParams = copyMultiMap(commonRefreshGrantParams);
        refreshGrantParams.add(OidcConstants.REFRESH_TOKEN_VALUE, refreshToken);
        return getJsonResponse(refreshGrantParams, Collections.emptyMap(), true);
    }

    private Uni<Tokens> getJsonResponse(MultiMap formBody, Map<String, String> additionalGrantParameters, boolean refresh) {
        //Uni needs to be lazy by default, we don't send the request unless
        //something has subscribed to it. This is important for the CAS state
        //management in TokensHelper
        return Uni.createFrom().deferred(new Supplier<Uni<? extends Tokens>>() {
            @Override
            public Uni<Tokens> get() {
                MultiMap body = formBody;
                HttpRequest<Buffer> request = client.postAbs(tokenRequestUri);
                request.putHeader(HttpHeaders.CONTENT_TYPE.toString(),
                        HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());
                if (clientSecretBasicAuthScheme != null) {
                    request.putHeader(AUTHORIZATION_HEADER, clientSecretBasicAuthScheme);
                } else if (clientJwtKey != null) {
                    // if it is a refresh then a map has already been copied
                    body = !refresh ? copyMultiMap(body) : body;
                    body.add(OidcConstants.CLIENT_ASSERTION_TYPE, OidcConstants.JWT_BEARER_CLIENT_ASSERTION_TYPE);
                    body.add(OidcConstants.CLIENT_ASSERTION, OidcCommonUtils.signJwtWithKey(oidcConfig, clientJwtKey));
                }
                if (!additionalGrantParameters.isEmpty()) {
                    body = copyMultiMap(body);
                    for (Map.Entry<String, String> entry : additionalGrantParameters.entrySet()) {
                        body.add(entry.getKey(), entry.getValue());
                    }
                }
                // Retry up to three times with a one second delay between the retries if the connection is closed
                Uni<HttpResponse<Buffer>> response = request.sendBuffer(OidcCommonUtils.encodeForm(body))
                        .onFailure(ConnectException.class)
                        .retry()
                        .atMost(oidcConfig.connectionRetryCount)
                        .onFailure().transform(t -> t.getCause());
                return response.onItem()
                        .transform(resp -> emitGrantTokens(resp, refresh));
            }
        });
    }

    private Tokens emitGrantTokens(HttpResponse<Buffer> resp, boolean refresh) {
        if (resp.statusCode() == 200) {
            LOG.debugf("%s OidcClient has %s the tokens", oidcConfig.getId().get(), (refresh ? "refreshed" : "acquired"));
            JsonObject json = resp.bodyAsJsonObject();
            final String accessToken = json.getString(oidcConfig.grant.accessTokenProperty);
            final String refreshToken = json.getString(oidcConfig.grant.refreshTokenProperty);
            final Object expiresInValue = json.getValue(oidcConfig.grant.expiresInProperty);
            Long accessTokenExpiresAt;
            if (expiresInValue != null) {
                long accessTokenExpiresIn = expiresInValue instanceof Number ? ((Number) expiresInValue).longValue()
                        : Long.parseLong(expiresInValue.toString());
                accessTokenExpiresAt = Instant.now().getEpochSecond() + accessTokenExpiresIn;
            } else {
                accessTokenExpiresAt = getExpiresJwtClaim(accessToken);
            }
            return new Tokens(accessToken, accessTokenExpiresAt, oidcConfig.refreshTokenTimeSkew.orElse(null), refreshToken,
                    json);
        } else {
            String errorMessage = resp.bodyAsString();
            LOG.debugf("%s OidcClient has failed to complete the %s grant request:  status: %d, error message: %s",
                    oidcConfig.getId().get(), (refresh ? OidcConstants.REFRESH_TOKEN_GRANT : grantType), resp.statusCode(),
                    errorMessage);
            throw new OidcClientException(errorMessage);
        }
    }

    private static Long getExpiresJwtClaim(String accessToken) {
        JsonObject claims = decodeJwtToken(accessToken);
        if (claims != null) {
            try {
                return claims.getLong(Claims.exp.name());
            } catch (IllegalArgumentException ex) {
                LOG.debug("JWT expiry claim can not be converted to Long");
            }
        }
        return null;
    }

    private static JsonObject decodeJwtToken(String accessToken) {
        String[] parts = accessToken.split("\\.");
        if (parts.length == 3) {
            try {
                return new JsonObject(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ex) {
                LOG.debug("JWT token can not be decoded using the Base64Url encoding scheme");
            }
        } else {
            LOG.debug("Access token is not formatted as the encoded JWT token");
        }
        return null;
    }

    private static MultiMap copyMultiMap(MultiMap oldMap) {
        MultiMap newMap = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
        newMap.addAll(oldMap);
        return newMap;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            client.close();
            closed = true;
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("OidcClient " + oidcConfig.getId().get() + " is closed");
        }
    }
}
