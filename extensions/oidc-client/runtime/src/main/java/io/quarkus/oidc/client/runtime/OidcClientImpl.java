package io.quarkus.oidc.client.runtime;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials.Jwt.Source;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniOnItem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.DecodeException;
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
    private final String tokenRevokeUri;
    private final MultiMap tokenGrantParams;
    private final MultiMap commonRefreshGrantParams;
    private final String grantType;
    private final String clientSecretBasicAuthScheme;
    private final Key clientJwtKey;
    private final boolean jwtBearerAuthentication;
    private final OidcClientConfig oidcConfig;
    private final Map<OidcEndpoint.Type, List<OidcRequestFilter>> filters;
    private volatile boolean closed;

    public OidcClientImpl(WebClient client, String tokenRequestUri, String tokenRevokeUri, String grantType,
            MultiMap tokenGrantParams, MultiMap commonRefreshGrantParams, OidcClientConfig oidcClientConfig,
            Map<OidcEndpoint.Type, List<OidcRequestFilter>> filters) {
        this.client = client;
        this.tokenRequestUri = tokenRequestUri;
        this.tokenRevokeUri = tokenRevokeUri;
        this.tokenGrantParams = tokenGrantParams;
        this.commonRefreshGrantParams = commonRefreshGrantParams;
        this.grantType = grantType;
        this.oidcConfig = oidcClientConfig;
        this.filters = filters;
        this.clientSecretBasicAuthScheme = OidcCommonUtils.initClientSecretBasicAuth(oidcClientConfig);
        this.jwtBearerAuthentication = oidcClientConfig.credentials.jwt.source == Source.BEARER;
        this.clientJwtKey = jwtBearerAuthentication ? null : OidcCommonUtils.initClientJwtKey(oidcClientConfig);
    }

    @Override
    public Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters) {
        checkClosed();
        if (tokenGrantParams == null) {
            throw new OidcClientException(
                    "Only 'refresh_token' grant is supported, please call OidcClient#refreshTokens method instead");
        }
        return getJsonResponse(OidcEndpoint.Type.TOKEN, tokenGrantParams, additionalGrantParameters, false);
    }

    @Override
    public Uni<Tokens> refreshTokens(String refreshToken, Map<String, String> additionalGrantParameters) {
        checkClosed();
        if (refreshToken == null) {
            throw new OidcClientException("Refresh token is null");
        }
        MultiMap refreshGrantParams = copyMultiMap(commonRefreshGrantParams);
        refreshGrantParams.add(OidcConstants.REFRESH_TOKEN_VALUE, refreshToken);
        return getJsonResponse(OidcEndpoint.Type.TOKEN, refreshGrantParams, additionalGrantParameters, true);
    }

    @Override
    public Uni<Boolean> revokeAccessToken(String accessToken, Map<String, String> additionalParameters) {
        checkClosed();
        if (accessToken == null) {
            throw new OidcClientException("Access token is null");
        }
        if (tokenRevokeUri != null) {
            MultiMap tokenRevokeParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
            tokenRevokeParams.set(OidcConstants.REVOCATION_TOKEN, accessToken);
            return postRequest(OidcEndpoint.Type.TOKEN_REVOCATION, client.postAbs(tokenRevokeUri), tokenRevokeParams,
                    additionalParameters, false)
                    .transform(resp -> toRevokeResponse(resp));
        } else {
            LOG.debugf("%s OidcClient can not revoke the access token because the revocation endpoint URL is not set");
            return Uni.createFrom().item(false);
        }

    }

    private Boolean toRevokeResponse(HttpResponse<Buffer> resp) {
        // Per RFC7009, 200 is returned if a token has been revoked successfully or if the client submitted an
        // invalid token, https://datatracker.ietf.org/doc/html/rfc7009#section-2.2.
        // 503 is at least theoretically possible if the OIDC server declines and suggests to Retry-After some period of time.
        // However this period of time can be set to unpredictable value.
        return resp.statusCode() == 503 ? false : true;
    }

    private Uni<Tokens> getJsonResponse(OidcEndpoint.Type endpointType, MultiMap formBody,
            Map<String, String> additionalGrantParameters,
            boolean refresh) {
        //Uni needs to be lazy by default, we don't send the request unless
        //something has subscribed to it. This is important for the CAS state
        //management in TokensHelper
        return Uni.createFrom().deferred(new Supplier<Uni<? extends Tokens>>() {
            @Override
            public Uni<Tokens> get() {
                return postRequest(endpointType, client.postAbs(tokenRequestUri), formBody, additionalGrantParameters, refresh)
                        .transform(resp -> emitGrantTokens(resp, refresh));
            }
        });
    }

    private UniOnItem<HttpResponse<Buffer>> postRequest(OidcEndpoint.Type endpointType, HttpRequest<Buffer> request,
            MultiMap formBody,
            Map<String, String> additionalGrantParameters,
            boolean refresh) {
        MultiMap body = formBody;
        request.putHeader(HttpHeaders.CONTENT_TYPE.toString(),
                HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());
        if (oidcConfig.headers != null) {
            for (Map.Entry<String, String> headerEntry : oidcConfig.headers.entrySet()) {
                request.putHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        if (clientSecretBasicAuthScheme != null) {
            request.putHeader(AUTHORIZATION_HEADER, clientSecretBasicAuthScheme);
        } else if (jwtBearerAuthentication) {
            if (!additionalGrantParameters.containsKey(OidcConstants.CLIENT_ASSERTION)) {
                String errorMessage = String.format(
                        "%s OidcClient can not complete the %s grant request because a JWT bearer client_assertion is missing",
                        oidcConfig.getId().get(), (refresh ? OidcConstants.REFRESH_TOKEN_GRANT : grantType));
                LOG.error(errorMessage);
                throw new OidcClientException(errorMessage);
            }
            body.add(OidcConstants.CLIENT_ASSERTION_TYPE, OidcConstants.JWT_BEARER_CLIENT_ASSERTION_TYPE);
        } else if (clientJwtKey != null) {
            // if it is a refresh then a map has already been copied
            body = !refresh ? copyMultiMap(body) : body;
            String jwt = OidcCommonUtils.signJwtWithKey(oidcConfig, tokenRequestUri, clientJwtKey);

            if (OidcCommonUtils.isClientSecretPostJwtAuthRequired(oidcConfig.credentials)) {
                body.add(OidcConstants.CLIENT_ID, oidcConfig.clientId.get());
                body.add(OidcConstants.CLIENT_SECRET, jwt);
            } else {
                body.add(OidcConstants.CLIENT_ASSERTION_TYPE, OidcConstants.JWT_BEARER_CLIENT_ASSERTION_TYPE);
                body.add(OidcConstants.CLIENT_ASSERTION, jwt);
            }
        } else if (OidcCommonUtils.isClientSecretPostAuthRequired(oidcConfig.credentials)) {
            body = !refresh ? copyMultiMap(body) : body;
            body.set(OidcConstants.CLIENT_ID, oidcConfig.clientId.get());
            body.set(OidcConstants.CLIENT_SECRET, OidcCommonUtils.clientSecret(oidcConfig.credentials));
        } else {
            body = !refresh ? copyMultiMap(body) : body;
            body = copyMultiMap(body).set(OidcConstants.CLIENT_ID, oidcConfig.clientId.get());
        }
        if (!additionalGrantParameters.isEmpty()) {
            body = copyMultiMap(body);
            for (Map.Entry<String, String> entry : additionalGrantParameters.entrySet()) {
                body.add(entry.getKey(), entry.getValue());
            }
        }
        // Retry up to three times with a one-second delay between the retries if the connection is closed
        Buffer buffer = OidcCommonUtils.encodeForm(body);
        Uni<HttpResponse<Buffer>> response = filter(endpointType, request, buffer).sendBuffer(buffer)
                .onFailure(ConnectException.class)
                .retry()
                .atMost(oidcConfig.connectionRetryCount)
                .onFailure().transform(t -> {
                    LOG.warn("OIDC Server is not available:", t.getCause() != null ? t.getCause() : t);
                    // don't wrap it to avoid information leak
                    return new OidcClientException("OIDC Server is not available");
                });
        return response.onItem();
    }

    private Tokens emitGrantTokens(HttpResponse<Buffer> resp, boolean refresh) {
        if (resp.statusCode() == 200) {
            LOG.debugf("%s OidcClient has %s the tokens", oidcConfig.getId().get(), (refresh ? "refreshed" : "acquired"));
            JsonObject json = resp.bodyAsJsonObject();
            // access token
            final String accessToken = json.getString(oidcConfig.grant.accessTokenProperty);
            final Long accessTokenExpiresAt = getExpiresAtValue(accessToken, json.getValue(oidcConfig.grant.expiresInProperty));

            final String refreshToken = json.getString(oidcConfig.grant.refreshTokenProperty);
            final Long refreshTokenExpiresAt = getExpiresAtValue(refreshToken,
                    json.getValue(oidcConfig.grant.refreshExpiresInProperty));

            return new Tokens(accessToken, accessTokenExpiresAt, oidcConfig.refreshTokenTimeSkew.orElse(null), refreshToken,
                    refreshTokenExpiresAt, json);
        } else {
            String errorMessage = resp.bodyAsString();
            LOG.debugf("%s OidcClient has failed to complete the %s grant request:  status: %d, error message: %s",
                    oidcConfig.getId().get(), (refresh ? OidcConstants.REFRESH_TOKEN_GRANT : grantType), resp.statusCode(),
                    errorMessage);
            throw new OidcClientException(errorMessage);
        }
    }

    private Long getExpiresAtValue(String token, Object expiresInValue) {
        if (expiresInValue != null) {
            long tokenExpiresIn = expiresInValue instanceof Number ? ((Number) expiresInValue).longValue()
                    : Long.parseLong(expiresInValue.toString());
            return oidcConfig.absoluteExpiresIn ? tokenExpiresIn
                    : Instant.now().getEpochSecond() + tokenExpiresIn;
        } else {
            return token != null ? getExpiresJwtClaim(token) : null;
        }
    }

    private static Long getExpiresJwtClaim(String token) {
        JsonObject claims = decodeJwtToken(token);
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
            } catch (DecodeException ex) {
                LOG.debug("JWT token can not be decoded");
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

    private HttpRequest<Buffer> filter(OidcEndpoint.Type endpointType, HttpRequest<Buffer> request, Buffer body) {
        if (!filters.isEmpty()) {
            OidcRequestContextProperties props = new OidcRequestContextProperties();
            for (OidcRequestFilter filter : OidcCommonUtils.getMatchingOidcRequestFilters(filters, endpointType)) {
                filter.filter(request, body, props);
            }
        }
        return request;
    }
}
