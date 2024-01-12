package io.quarkus.oidc.runtime;

import java.io.Closeable;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials.Secret.Method;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.common.runtime.OidcEndpointAccessException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniOnItem;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcProviderClient implements Closeable {
    private static final Logger LOG = Logger.getLogger(OidcProviderClient.class);

    private static final String AUTHORIZATION_HEADER = String.valueOf(HttpHeaders.AUTHORIZATION);
    private static final String CONTENT_TYPE_HEADER = String.valueOf(HttpHeaders.CONTENT_TYPE);
    private static final String ACCEPT_HEADER = String.valueOf(HttpHeaders.ACCEPT);
    private static final String APPLICATION_X_WWW_FORM_URLENCODED = String
            .valueOf(HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());
    private static final String APPLICATION_JSON = "application/json";

    private final WebClient client;
    private final Vertx vertx;
    private final OidcConfigurationMetadata metadata;
    private final OidcTenantConfig oidcConfig;
    private final String clientSecretBasicAuthScheme;
    private final String introspectionBasicAuthScheme;
    private final Key clientJwtKey;
    private final Map<OidcEndpoint.Type, List<OidcRequestFilter>> filters;
    private final boolean clientSecretQueryAuthentication;

    public OidcProviderClient(WebClient client,
            Vertx vertx,
            OidcConfigurationMetadata metadata,
            OidcTenantConfig oidcConfig,
            Map<OidcEndpoint.Type, List<OidcRequestFilter>> filters) {
        this.client = client;
        this.vertx = vertx;
        this.metadata = metadata;
        this.oidcConfig = oidcConfig;
        this.clientSecretBasicAuthScheme = OidcCommonUtils.initClientSecretBasicAuth(oidcConfig);
        this.clientJwtKey = OidcCommonUtils.initClientJwtKey(oidcConfig);
        this.introspectionBasicAuthScheme = initIntrospectionBasicAuthScheme(oidcConfig);
        this.filters = filters;
        this.clientSecretQueryAuthentication = oidcConfig.credentials.clientSecret.method.orElse(null) == Method.QUERY;
    }

    private static String initIntrospectionBasicAuthScheme(OidcTenantConfig oidcConfig) {
        if (oidcConfig.getIntrospectionCredentials().name.isPresent()
                && oidcConfig.getIntrospectionCredentials().secret.isPresent()) {
            return OidcCommonUtils.basicSchemeValue(oidcConfig.getIntrospectionCredentials().name.get(),
                    oidcConfig.getIntrospectionCredentials().secret.get());
        } else {
            return null;
        }
    }

    public OidcConfigurationMetadata getMetadata() {
        return metadata;
    }

    public Uni<JsonWebKeySet> getJsonWebKeySet(OidcRequestContextProperties contextProperties) {
        return filter(OidcEndpoint.Type.JWKS, client.getAbs(metadata.getJsonWebKeySetUri()), null, contextProperties).send()
                .onItem()
                .transform(resp -> getJsonWebKeySet(resp));
    }

    public Uni<UserInfo> getUserInfo(String token) {
        LOG.debugf("Get UserInfo on: %s auth: %s", metadata.getUserInfoUri(), OidcConstants.BEARER_SCHEME + " " + token);
        return filter(OidcEndpoint.Type.USERINFO, client.getAbs(metadata.getUserInfoUri()), null, null)
                .putHeader(AUTHORIZATION_HEADER, OidcConstants.BEARER_SCHEME + " " + token)
                .send().onItem().transform(resp -> getUserInfo(resp));
    }

    public Uni<TokenIntrospection> introspectToken(String token) {
        MultiMap introspectionParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
        introspectionParams.add(OidcConstants.INTROSPECTION_TOKEN, token);
        introspectionParams.add(OidcConstants.INTROSPECTION_TOKEN_TYPE_HINT, OidcConstants.ACCESS_TOKEN_VALUE);
        return getHttpResponse(metadata.getIntrospectionUri(), introspectionParams, true)
                .transform(resp -> getTokenIntrospection(resp));
    }

    private JsonWebKeySet getJsonWebKeySet(HttpResponse<Buffer> resp) {
        if (resp.statusCode() == 200) {
            return new JsonWebKeySet(resp.bodyAsString(StandardCharsets.UTF_8.name()));
        } else {
            throw new OidcEndpointAccessException(resp.statusCode());
        }
    }

    public OidcTenantConfig getOidcConfig() {
        return oidcConfig;
    }

    public Uni<AuthorizationCodeTokens> getAuthorizationCodeTokens(String code, String redirectUri, String codeVerifier) {
        MultiMap codeGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
        codeGrantParams.add(OidcConstants.GRANT_TYPE, OidcConstants.AUTHORIZATION_CODE);
        codeGrantParams.add(OidcConstants.CODE_FLOW_CODE, code);
        codeGrantParams.add(OidcConstants.CODE_FLOW_REDIRECT_URI, redirectUri);
        if (codeVerifier != null) {
            codeGrantParams.add(OidcConstants.PKCE_CODE_VERIFIER, codeVerifier);
        }
        if (oidcConfig.codeGrant.extraParams != null) {
            codeGrantParams.addAll(oidcConfig.codeGrant.extraParams);
        }
        return getHttpResponse(metadata.getTokenUri(), codeGrantParams, false)
                .transform(resp -> getAuthorizationCodeTokens(resp));
    }

    public Uni<AuthorizationCodeTokens> refreshAuthorizationCodeTokens(String refreshToken) {
        MultiMap refreshGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
        refreshGrantParams.add(OidcConstants.GRANT_TYPE, OidcConstants.REFRESH_TOKEN_GRANT);
        refreshGrantParams.add(OidcConstants.REFRESH_TOKEN_VALUE, refreshToken);
        return getHttpResponse(metadata.getTokenUri(), refreshGrantParams, false)
                .transform(resp -> getAuthorizationCodeTokens(resp));
    }

    private UniOnItem<HttpResponse<Buffer>> getHttpResponse(String uri, MultiMap formBody, boolean introspect) {
        HttpRequest<Buffer> request = client.postAbs(uri);

        Buffer buffer = null;

        if (!clientSecretQueryAuthentication) {
            request.putHeader(CONTENT_TYPE_HEADER, APPLICATION_X_WWW_FORM_URLENCODED);
            request.putHeader(ACCEPT_HEADER, APPLICATION_JSON);

            if (introspect && introspectionBasicAuthScheme != null) {
                request.putHeader(AUTHORIZATION_HEADER, introspectionBasicAuthScheme);
                if (oidcConfig.clientId.isPresent() && oidcConfig.introspectionCredentials.includeClientId) {
                    formBody.set(OidcConstants.CLIENT_ID, oidcConfig.clientId.get());
                }
            } else if (clientSecretBasicAuthScheme != null) {
                request.putHeader(AUTHORIZATION_HEADER, clientSecretBasicAuthScheme);
            } else if (clientJwtKey != null) {
                String jwt = OidcCommonUtils.signJwtWithKey(oidcConfig, metadata.getTokenUri(), clientJwtKey);
                if (OidcCommonUtils.isClientSecretPostJwtAuthRequired(oidcConfig.credentials)) {
                    formBody.add(OidcConstants.CLIENT_ID, oidcConfig.clientId.get());
                    formBody.add(OidcConstants.CLIENT_SECRET, jwt);
                } else {
                    formBody.add(OidcConstants.CLIENT_ASSERTION_TYPE, OidcConstants.JWT_BEARER_CLIENT_ASSERTION_TYPE);
                    formBody.add(OidcConstants.CLIENT_ASSERTION, jwt);
                }
            } else if (OidcCommonUtils.isClientSecretPostAuthRequired(oidcConfig.credentials)) {
                formBody.add(OidcConstants.CLIENT_ID, oidcConfig.clientId.get());
                formBody.add(OidcConstants.CLIENT_SECRET, OidcCommonUtils.clientSecret(oidcConfig.credentials));
            } else {
                formBody.add(OidcConstants.CLIENT_ID, oidcConfig.clientId.get());
            }
            buffer = OidcCommonUtils.encodeForm(formBody);
        } else {
            formBody.add(OidcConstants.CLIENT_ID, oidcConfig.clientId.get());
            formBody.add(OidcConstants.CLIENT_SECRET, OidcCommonUtils.clientSecret(oidcConfig.credentials));
            for (Map.Entry<String, String> entry : formBody) {
                request.addQueryParam(entry.getKey(), OidcCommonUtils.urlEncode(entry.getValue()));
            }
            request.putHeader(ACCEPT_HEADER, APPLICATION_JSON);
            buffer = Buffer.buffer();
        }

        if (oidcConfig.codeGrant.headers != null) {
            for (Map.Entry<String, String> headerEntry : oidcConfig.codeGrant.headers.entrySet()) {
                request.putHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }

        LOG.debugf("Get token on: %s params: %s headers: %s", metadata.getTokenUri(), formBody, request.headers());
        // Retry up to three times with a one-second delay between the retries if the connection is closed.

        OidcEndpoint.Type endpoint = introspect ? OidcEndpoint.Type.INTROSPECTION : OidcEndpoint.Type.TOKEN;
        Uni<HttpResponse<Buffer>> response = filter(endpoint, request, buffer, null).sendBuffer(buffer)
                .onFailure(ConnectException.class)
                .retry()
                .atMost(oidcConfig.connectionRetryCount).onFailure().transform(t -> t.getCause());
        return response.onItem();

    }

    private AuthorizationCodeTokens getAuthorizationCodeTokens(HttpResponse<Buffer> resp) {
        JsonObject json = getJsonObject(resp);
        final String idToken = json.getString(OidcConstants.ID_TOKEN_VALUE);
        final String accessToken = json.getString(OidcConstants.ACCESS_TOKEN_VALUE);
        final String refreshToken = json.getString(OidcConstants.REFRESH_TOKEN_VALUE);
        return new AuthorizationCodeTokens(idToken, accessToken, refreshToken);
    }

    private UserInfo getUserInfo(HttpResponse<Buffer> resp) {
        return new UserInfo(getString(resp));
    }

    private TokenIntrospection getTokenIntrospection(HttpResponse<Buffer> resp) {
        return new TokenIntrospection(getString(resp));
    }

    private static JsonObject getJsonObject(HttpResponse<Buffer> resp) {
        if (resp.statusCode() == 200) {
            LOG.debugf("Request succeeded: %s", resp.bodyAsJsonObject());
            return resp.bodyAsJsonObject();
        } else {
            throw responseException(resp);
        }
    }

    private static String getString(HttpResponse<Buffer> resp) {
        if (resp.statusCode() == 200) {
            LOG.debugf("Request succeeded: %s", resp.bodyAsString());
            return resp.bodyAsString();
        } else {
            throw responseException(resp);
        }
    }

    private static OIDCException responseException(HttpResponse<Buffer> resp) {
        String errorMessage = resp.bodyAsString();
        LOG.debugf("Request has failed: status: %d, error message: %s", resp.statusCode(), errorMessage);
        throw new OIDCException(errorMessage);
    }

    @Override
    public void close() {
        client.close();
    }

    public Key getClientJwtKey() {
        return clientJwtKey;
    }

    private HttpRequest<Buffer> filter(OidcEndpoint.Type endpointType, HttpRequest<Buffer> request, Buffer body,
            OidcRequestContextProperties contextProperties) {
        if (!filters.isEmpty()) {
            Map<String, Object> newProperties = contextProperties == null ? new HashMap<>()
                    : new HashMap<>(contextProperties.getAll());
            newProperties.put(OidcConfigurationMetadata.class.getName(), metadata);
            OidcRequestContextProperties newContextProperties = new OidcRequestContextProperties(newProperties);
            for (OidcRequestFilter filter : OidcCommonUtils.getMatchingOidcRequestFilters(filters, endpointType)) {
                filter.filter(request, body, newContextProperties);
            }
        }
        return request;
    }

    public Vertx getVertx() {
        return vertx;
    }
}
