package io.quarkus.oidc.runtime;

import java.io.Closeable;
import java.net.SocketException;
import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.jose4j.lang.UnresolvableKeyException;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcProviderClient;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.ClientAssertionProvider;
import io.quarkus.oidc.common.runtime.OidcClientRedirectException;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret.Method;
import io.quarkus.security.credential.TokenCredential;
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

public class OidcProviderClientImpl implements OidcProviderClient, Closeable {

    private enum TokenOperation {
        GET("Get"),
        REFRESH("Refresh"),
        INTROSPECT("Introspect"),
        REVOKE("Revoke");

        String op;

        TokenOperation(String op) {
            this.op = op;
        }

        String operation() {
            return op;
        }
    }

    private static final Logger LOG = Logger.getLogger(OidcProviderClientImpl.class);

    private static final String AUTHORIZATION_HEADER = String.valueOf(HttpHeaders.AUTHORIZATION);
    private static final String CONTENT_TYPE_HEADER = String.valueOf(HttpHeaders.CONTENT_TYPE);
    private static final String ACCEPT_HEADER = String.valueOf(HttpHeaders.ACCEPT);
    private static final String APPLICATION_X_WWW_FORM_URLENCODED = HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString();
    private static final String APPLICATION_JSON = "application/json";

    private final WebClient client;
    private final Vertx vertx;
    private final OidcConfigurationMetadata metadata;
    private final OidcTenantConfig oidcConfig;
    private final String introspectionBasicAuthScheme;
    private final Key clientJwtKey;
    private final boolean jwtBearerAuthentication;
    private final ClientAssertionProvider clientAssertionProvider;
    private final Map<OidcEndpoint.Type, List<OidcRequestFilter>> requestFilters;
    private final Map<OidcEndpoint.Type, List<OidcResponseFilter>> responseFilters;
    private final boolean clientSecretQueryAuthentication;
    private final String jwtSecret;
    private volatile String clientSecret;
    private volatile String clientSecretBasicAuthScheme;

    private OidcProvider oidcProvider;

    private OidcProviderClientImpl(WebClient client, Vertx vertx, OidcConfigurationMetadata metadata,
            OidcTenantConfig oidcConfig, ClientCredentials clientCredentials,
            Map<OidcEndpoint.Type, List<OidcRequestFilter>> requestFilters,
            Map<OidcEndpoint.Type, List<OidcResponseFilter>> responseFilters) {
        this.client = client;
        this.vertx = vertx;
        this.metadata = metadata;
        this.oidcConfig = oidcConfig;
        this.clientSecretBasicAuthScheme = clientCredentials.clientSecretBasicAuthScheme;
        this.jwtBearerAuthentication = oidcConfig.credentials().jwt()
                .source() == OidcClientCommonConfig.Credentials.Jwt.Source.BEARER;
        this.clientAssertionProvider = this.jwtBearerAuthentication ? createClientAssertionProvider(vertx, oidcConfig) : null;
        this.clientJwtKey = jwtBearerAuthentication ? null : clientCredentials.clientJwtKey;
        this.introspectionBasicAuthScheme = initIntrospectionBasicAuthScheme(oidcConfig);
        this.requestFilters = requestFilters;
        this.responseFilters = responseFilters;
        this.clientSecretQueryAuthentication = oidcConfig.credentials().clientSecret().method().orElse(null) == Method.QUERY;
        this.clientSecret = clientCredentials.clientSecret;
        this.jwtSecret = clientCredentials.jwtSecret;
    }

    private static ClientAssertionProvider createClientAssertionProvider(Vertx vertx, OidcTenantConfig oidcConfig) {
        var clientAssertionProvider = new ClientAssertionProvider(vertx,
                oidcConfig.credentials().jwt().tokenPath().get());
        if (clientAssertionProvider.getClientAssertion() == null) {
            throw new OIDCException("Cannot find a valid JWT bearer token at path: "
                    + oidcConfig.credentials().jwt().tokenPath().get());
        }
        return clientAssertionProvider;
    }

    void setOidcProvider(OidcProvider oidcProvider) {
        this.oidcProvider = oidcProvider;
    }

    private static String initIntrospectionBasicAuthScheme(OidcTenantConfig oidcConfig) {
        if (oidcConfig.introspectionCredentials().name().isPresent()
                && oidcConfig.introspectionCredentials().secret().isPresent()) {
            return OidcCommonUtils.basicSchemeValue(oidcConfig.introspectionCredentials().name().get(),
                    oidcConfig.introspectionCredentials().secret().get());
        } else {
            return null;
        }
    }

    OidcConfigurationMetadata getMetadata() {
        return metadata;
    }

    Uni<JsonWebKeySet> getJsonWebKeySet(OidcRequestContextProperties contextProperties) {
        final OidcRequestContextProperties requestProps = getRequestProps(contextProperties);
        return doGetJsonWebKeySet(requestProps, List.of())
                .onFailure(OidcCommonUtils.validOidcClientRedirect(metadata.getJsonWebKeySetUri()))
                .recoverWithUni(
                        new Function<Throwable, Uni<? extends JsonWebKeySet>>() {
                            @Override
                            public Uni<JsonWebKeySet> apply(Throwable t) {
                                OidcClientRedirectException ex = (OidcClientRedirectException) t;
                                return doGetJsonWebKeySet(requestProps, ex.getCookies());
                            }
                        });
    }

    private Uni<JsonWebKeySet> doGetJsonWebKeySet(OidcRequestContextProperties requestProps, List<String> cookies) {
        LOG.debugf("Get verification JWT Key Set at %s", metadata.getJsonWebKeySetUri());
        HttpRequest<Buffer> request = client.getAbs(metadata.getJsonWebKeySetUri());
        if (!cookies.isEmpty()) {
            request.putHeader(OidcCommonUtils.COOKIE_REQUEST_HEADER, cookies);
        }
        return filterHttpRequest(requestProps, OidcEndpoint.Type.JWKS, request, null)
                .flatMap(httpRequest -> OidcCommonUtils
                        .sendRequest(vertx, httpRequest, oidcConfig.useBlockingDnsLookup()))
                .onItem()
                .transformToUni(resp -> getJsonWebKeySet(requestProps, resp));
    }

    public Uni<UserInfo> getUserInfo(final String accessToken) {

        final OidcRequestContextProperties requestProps = getRequestProps(null, null);

        Uni<UserInfoResponse> response = doGetUserInfo(requestProps, accessToken, List.of())
                .onFailure(OidcCommonUtils.validOidcClientRedirect(metadata.getUserInfoUri()))
                .recoverWithUni(
                        new Function<Throwable, Uni<? extends UserInfoResponse>>() {
                            @Override
                            public Uni<UserInfoResponse> apply(Throwable t) {
                                OidcClientRedirectException ex = (OidcClientRedirectException) t;
                                return doGetUserInfo(requestProps, accessToken, ex.getCookies());
                            }
                        });
        return response.onItem()
                .transformToUni(new Function<UserInfoResponse, Uni<? extends UserInfo>>() {

                    @Override
                    public Uni<UserInfo> apply(UserInfoResponse response) {
                        if (OidcUtils.isApplicationJwtContentType(response.contentType())) {
                            if (oidcConfig.jwks().resolveEarly()) {
                                try {
                                    LOG.debugf("Verifying the signed UserInfo with the local JWK keys: %s", response.data());
                                    return Uni.createFrom().item(
                                            new UserInfo(
                                                    oidcProvider.verifyJwtToken(response.data(), true, false,
                                                            null).localVerificationResult
                                                            .encode()));
                                } catch (Throwable t) {
                                    if (t.getCause() instanceof UnresolvableKeyException) {
                                        LOG.debug(
                                                "No matching JWK key is found, refreshing and repeating the signed UserInfo verification");
                                        return oidcProvider.refreshJwksAndVerifyJwtToken(response.data(), true, false, null)
                                                .onItem().transform(v -> new UserInfo(v.localVerificationResult.encode()));
                                    } else {
                                        LOG.debugf("Signed UserInfo verification has failed: %s", t.getMessage());
                                        return Uni.createFrom().failure(t);
                                    }
                                }
                            } else {
                                return oidcProvider
                                        .getKeyResolverAndVerifyJwtToken(new TokenCredential(response.data(), "userinfo"), true,
                                                false, null, true)
                                        .onItem().transform(v -> new UserInfo(v.localVerificationResult.encode()));
                            }
                        } else {
                            return Uni.createFrom().item(new UserInfo(response.data()));
                        }
                    }
                });
    }

    private Uni<UserInfoResponse> doGetUserInfo(OidcRequestContextProperties requestProps, String token, List<String> cookies) {
        LOG.debugf("Get UserInfo on: %s auth: %s", metadata.getUserInfoUri(), OidcConstants.BEARER_SCHEME + " " + token);

        HttpRequest<Buffer> request = client.getAbs(metadata.getUserInfoUri());
        if (!cookies.isEmpty()) {
            request.putHeader(OidcCommonUtils.COOKIE_REQUEST_HEADER, cookies);
        }
        return filterHttpRequest(requestProps, OidcEndpoint.Type.USERINFO, request, null)
                .flatMap(httpRequest -> OidcCommonUtils
                        .sendRequest(vertx,
                                httpRequest.putHeader(AUTHORIZATION_HEADER, OidcConstants.BEARER_SCHEME + " " + token),
                                oidcConfig.useBlockingDnsLookup()))
                .flatMap(resp -> getUserInfo(requestProps, resp));
    }

    public Uni<TokenIntrospection> introspectAccessToken(final String token) {
        final MultiMap introspectionParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
        introspectionParams.add(OidcConstants.INTROSPECTION_TOKEN, token);
        introspectionParams.add(OidcConstants.INTROSPECTION_TOKEN_TYPE_HINT, OidcConstants.ACCESS_TOKEN_VALUE);
        final OidcRequestContextProperties requestProps = getRequestProps(null, null);
        return getHttpResponse(requestProps, metadata.getIntrospectionUri(), introspectionParams, TokenOperation.INTROSPECT,
                OidcEndpoint.Type.INTROSPECTION)
                .transformToUni(resp -> getTokenIntrospection(requestProps, resp));
    }

    private Uni<JsonWebKeySet> getJsonWebKeySet(OidcRequestContextProperties requestProps, HttpResponse<Buffer> resp) {
        return getString(requestProps, metadata.getJsonWebKeySetUri(), resp, OidcEndpoint.Type.JWKS).map(JsonWebKeySet::new);
    }

    Uni<AuthorizationCodeTokens> getAuthorizationCodeTokens(String code, String redirectUri, String codeVerifier) {
        final MultiMap codeGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
        codeGrantParams.add(OidcConstants.GRANT_TYPE, OidcConstants.AUTHORIZATION_CODE);
        codeGrantParams.add(OidcConstants.CODE_FLOW_CODE, code);
        codeGrantParams.add(OidcConstants.CODE_FLOW_REDIRECT_URI, redirectUri);
        if (codeVerifier != null) {
            codeGrantParams.add(OidcConstants.PKCE_CODE_VERIFIER, codeVerifier);
        }
        if (oidcConfig.codeGrant().extraParams() != null) {
            codeGrantParams.addAll(oidcConfig.codeGrant().extraParams());
        }
        final OidcRequestContextProperties requestProps = getRequestProps(OidcConstants.AUTHORIZATION_CODE);
        return getHttpResponse(requestProps, metadata.getTokenUri(), codeGrantParams, TokenOperation.GET,
                OidcEndpoint.Type.TOKEN)
                .transformToUni(resp -> getAuthorizationCodeTokens(requestProps, resp));
    }

    Uni<AuthorizationCodeTokens> refreshAuthorizationCodeTokens(String refreshToken) {
        final MultiMap refreshGrantParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
        refreshGrantParams.add(OidcConstants.GRANT_TYPE, OidcConstants.REFRESH_TOKEN_GRANT);
        refreshGrantParams.add(OidcConstants.REFRESH_TOKEN_VALUE, refreshToken);
        final OidcRequestContextProperties requestProps = getRequestProps(OidcConstants.REFRESH_TOKEN_GRANT);
        return getHttpResponse(requestProps, metadata.getTokenUri(), refreshGrantParams, TokenOperation.REFRESH,
                OidcEndpoint.Type.TOKEN)
                .transformToUni(resp -> getAuthorizationCodeTokens(requestProps, resp));
    }

    public Uni<Boolean> revokeAccessToken(String accessToken) {
        return revokeToken(accessToken, OidcConstants.ACCESS_TOKEN_VALUE);
    }

    public Uni<Boolean> revokeRefreshToken(String refreshToken) {
        return revokeToken(refreshToken, OidcConstants.REFRESH_TOKEN_VALUE);
    }

    private Uni<Boolean> revokeToken(String token, String tokenTypeHint) {

        if (metadata.getRevocationUri() != null) {
            OidcRequestContextProperties requestProps = getRequestProps(null, null);
            MultiMap tokenRevokeParams = new MultiMap(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
            tokenRevokeParams.set(OidcConstants.REVOCATION_TOKEN, token);
            tokenRevokeParams.set(OidcConstants.REVOCATION_TOKEN_TYPE_HINT, tokenTypeHint);

            return getHttpResponse(requestProps, metadata.getRevocationUri(), tokenRevokeParams, TokenOperation.REVOKE,
                    OidcEndpoint.Type.TOKEN_REVOCATION)
                    .transformToUni(resp -> toRevokeResponse(requestProps, resp));
        } else {
            LOG.debugf("The %s token can not be revoked because the revocation endpoint URL is not set", tokenTypeHint);
            return Uni.createFrom().item(false);
        }

    }

    private Uni<Boolean> toRevokeResponse(OidcRequestContextProperties requestProps, HttpResponse<Buffer> resp) {
        // Per RFC7009, 200 is returned if a token has been revoked successfully or if the client submitted an
        // invalid token, https://datatracker.ietf.org/doc/html/rfc7009#section-2.2.
        // 503 is at least theoretically possible if the OIDC server declines and suggests to Retry-After some period of time.
        // However this period of time can be set to unpredictable value.
        return OidcCommonUtils.filterHttpResponse(requestProps, resp, responseFilters, OidcEndpoint.Type.TOKEN_REVOCATION)
                .replaceWith(resp.statusCode() != 503);
    }

    private record PreparedHttpRequest(Uni<HttpResponse<Buffer>> httpRequestUni, CredentialsToRetry credentialsToRetry) {
        enum CredentialsToRetry {
            CLIENT_SECRET,
            CLIENT_SECRET_BASIC_AUTH_SCHEME
        }
    }

    private PreparedHttpRequest prepareHttpRequest(OidcRequestContextProperties requestProps, String uri,
            MultiMap formBody, TokenOperation op, OidcEndpoint.Type endpointType) {
        HttpRequest<Buffer> request = client.postAbs(uri);

        final Buffer buffer;
        PreparedHttpRequest.CredentialsToRetry credentialsToRetry = null;

        if (!clientSecretQueryAuthentication) {
            request.putHeader(CONTENT_TYPE_HEADER, APPLICATION_X_WWW_FORM_URLENCODED);
            request.putHeader(ACCEPT_HEADER, APPLICATION_JSON);

            if (isIntrospection(op) && introspectionBasicAuthScheme != null) {
                request.putHeader(AUTHORIZATION_HEADER, introspectionBasicAuthScheme);
                if (oidcConfig.clientId().isPresent() && oidcConfig.introspectionCredentials().includeClientId()) {
                    formBody.set(OidcConstants.CLIENT_ID, oidcConfig.clientId().get());
                }
            } else if (clientSecretBasicAuthScheme != null) {
                request.putHeader(AUTHORIZATION_HEADER, clientSecretBasicAuthScheme);
                if (hasClientSecretProvider()) {
                    credentialsToRetry = PreparedHttpRequest.CredentialsToRetry.CLIENT_SECRET_BASIC_AUTH_SCHEME;
                }
            } else if (jwtBearerAuthentication) {
                final String clientAssertion = clientAssertionProvider.getClientAssertion();
                if (clientAssertion == null) {
                    throw new OIDCException(String.format(
                            "Cannot get token for tenant '%s' because a JWT bearer client_assertion is not available",
                            oidcConfig.tenantId().get()));
                }
                formBody.add(OidcConstants.CLIENT_ASSERTION, clientAssertion);
                formBody.add(OidcConstants.CLIENT_ASSERTION_TYPE, OidcConstants.JWT_BEARER_CLIENT_ASSERTION_TYPE);
            } else if (clientJwtKey != null) {
                String jwt = OidcCommonUtils.signJwtWithKey(oidcConfig, metadata.getTokenUri(), clientJwtKey);
                if (OidcCommonUtils.isClientSecretPostJwtAuthRequired(oidcConfig.credentials())) {
                    formBody.add(OidcConstants.CLIENT_ID, oidcConfig.clientId().get());
                    formBody.add(OidcConstants.CLIENT_SECRET, jwt);
                } else {
                    formBody.add(OidcConstants.CLIENT_ASSERTION_TYPE, OidcConstants.JWT_BEARER_CLIENT_ASSERTION_TYPE);
                    formBody.add(OidcConstants.CLIENT_ASSERTION, jwt);
                }
            } else if (OidcCommonUtils.isClientSecretPostAuthRequired(oidcConfig.credentials())) {
                formBody.add(OidcConstants.CLIENT_ID, oidcConfig.clientId().get());
                formBody.add(OidcConstants.CLIENT_SECRET, clientSecret);
                if (hasClientSecretProvider()) {
                    credentialsToRetry = PreparedHttpRequest.CredentialsToRetry.CLIENT_SECRET;
                }
            } else {
                formBody.add(OidcConstants.CLIENT_ID, oidcConfig.clientId().get());
            }
            buffer = OidcCommonUtils.encodeForm(formBody);
        } else {
            formBody.add(OidcConstants.CLIENT_ID, oidcConfig.clientId().get());
            formBody.add(OidcConstants.CLIENT_SECRET, clientSecret);
            if (hasClientSecretProvider()) {
                credentialsToRetry = PreparedHttpRequest.CredentialsToRetry.CLIENT_SECRET;
            }
            for (Map.Entry<String, String> entry : formBody) {
                request.addQueryParam(entry.getKey(), OidcCommonUtils.urlEncode(entry.getValue()));
            }
            request.putHeader(ACCEPT_HEADER, APPLICATION_JSON);
            buffer = Buffer.buffer();
        }

        if (oidcConfig.codeGrant().headers() != null) {
            for (Map.Entry<String, String> headerEntry : oidcConfig.codeGrant().headers().entrySet()) {
                request.putHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debugf("%s token: url : %s, headers: %s, request params: %s", op.operation(), request.uri(), request.headers(),
                    formBody);
        }
        // Retry up to three times with a one-second delay between the retries if the connection is closed.
        var preparedResponse = filterHttpRequest(requestProps, endpointType, request, buffer)
                .flatMap(httpRequest -> httpRequest.sendBuffer(OidcCommonUtils.getRequestBuffer(requestProps, buffer)))
                .onFailure(SocketException.class)
                .retry()
                .atMost(oidcConfig.connectionRetryCount())
                .onFailure().transform(Throwable::getCause);
        return new PreparedHttpRequest(preparedResponse, credentialsToRetry);
    }

    private UniOnItem<HttpResponse<Buffer>> getHttpResponse(OidcRequestContextProperties requestProps, String uri,
            MultiMap formBody, TokenOperation op, OidcEndpoint.Type endpointType) {

        final MultiMap newFormBody;
        boolean hasClientSecretProvider = hasClientSecretProvider();
        if (hasClientSecretProvider) {
            // copy to avoid duplications on credentials refresh
            var delegate = io.vertx.core.MultiMap.caseInsensitiveMultiMap().addAll(formBody.getDelegate());
            newFormBody = new MultiMap(delegate);
        } else {
            newFormBody = formBody;
        }

        var preparedRequest = prepareHttpRequest(requestProps, uri, newFormBody, op, endpointType);
        if (hasClientSecretProvider && preparedRequest.credentialsToRetry != null) {
            return preparedRequest.httpRequestUni.flatMap(httpResponse -> {
                if (httpResponse.statusCode() == 401) {
                    // here we need to deal with error responses (like unauthorized_client) possibly caused by
                    // invalid credentialsToRetry; if credentialsToRetry provider updated credentialsToRetry, we should retry
                    var credentialsRefresh = switch (preparedRequest.credentialsToRetry) {
                        case CLIENT_SECRET -> OidcCommonUtils.clientSecret(oidcConfig.credentials())
                                .map(newClientSecret -> {
                                    if (newClientSecret != null && !newClientSecret.equals(clientSecret)) {
                                        this.clientSecret = newClientSecret;
                                        return true;
                                    }
                                    return false;
                                });
                        case CLIENT_SECRET_BASIC_AUTH_SCHEME -> OidcCommonUtils.clientSecret(oidcConfig.credentials())
                                .map(newClientSecret -> {
                                    var newClientSecretBasicAuthScheme = OidcCommonUtils.initClientSecretBasicAuth(oidcConfig,
                                            newClientSecret);
                                    if (newClientSecretBasicAuthScheme != null
                                            && !newClientSecretBasicAuthScheme.equals(clientSecretBasicAuthScheme)) {
                                        this.clientSecret = newClientSecret;
                                        this.clientSecretBasicAuthScheme = newClientSecretBasicAuthScheme;
                                        return true;
                                    }
                                    return false;
                                });
                    };

                    return credentialsRefresh.flatMap(credentialsRefreshed -> {
                        if (Boolean.TRUE.equals(credentialsRefreshed)) {
                            LOG.debug("HTTP request failed with response status code 401 and the CredentialsProvider"
                                    + " provided new credentials, retrying the request with new credentials");
                            return prepareHttpRequest(requestProps, uri, formBody, op, endpointType).httpRequestUni;
                        }
                        return Uni.createFrom().item(httpResponse);
                    });
                }
                return Uni.createFrom().item(httpResponse);
            }).onItem();
        }
        return preparedRequest.httpRequestUni.onItem();
    }

    private boolean hasClientSecretProvider() {
        return oidcConfig.credentials().clientSecret().provider().key().isPresent();
    }

    private Uni<AuthorizationCodeTokens> getAuthorizationCodeTokens(OidcRequestContextProperties requestProps,
            HttpResponse<Buffer> resp) {
        return getJsonObject(requestProps, metadata.getTokenUri(), resp, OidcEndpoint.Type.TOKEN)
                .map(json -> {
                    final String idToken = json.getString(OidcConstants.ID_TOKEN_VALUE);
                    final String accessToken = json.getString(OidcConstants.ACCESS_TOKEN_VALUE);
                    final String refreshToken = json.getString(OidcConstants.REFRESH_TOKEN_VALUE);
                    Long tokenExpiresIn = null;
                    Object tokenExpiresInObj = json.getValue(OidcConstants.EXPIRES_IN);
                    if (tokenExpiresInObj != null) {
                        tokenExpiresIn = tokenExpiresInObj instanceof Number ? ((Number) tokenExpiresInObj).longValue()
                                : Long.parseLong(tokenExpiresInObj.toString());
                    }
                    final String accessTokenScope = json.getString(OidcConstants.TOKEN_SCOPE);

                    return new AuthorizationCodeTokens(idToken, accessToken, refreshToken, tokenExpiresIn, accessTokenScope);
                });
    }

    private Uni<UserInfoResponse> getUserInfo(OidcRequestContextProperties requestProps, HttpResponse<Buffer> resp) {
        return getString(requestProps, metadata.getUserInfoUri(), resp, OidcEndpoint.Type.USERINFO)
                .map(str -> new UserInfoResponse(resp.getHeader(CONTENT_TYPE_HEADER), str));
    }

    private Uni<TokenIntrospection> getTokenIntrospection(OidcRequestContextProperties requestProps,
            HttpResponse<Buffer> resp) {
        return getString(requestProps, metadata.getIntrospectionUri(), resp, OidcEndpoint.Type.INTROSPECTION)
                .map(TokenIntrospection::new);
    }

    private Uni<JsonObject> getJsonObject(OidcRequestContextProperties requestProps, String requestUri,
            HttpResponse<Buffer> resp,
            OidcEndpoint.Type endpoint) {
        return OidcCommonUtils.filterHttpResponse(requestProps, resp, responseFilters, endpoint)
                .flatMap(buffer -> {
                    if (resp.statusCode() == 200) {
                        LOG.debugf("Request succeeded: %s", resp.bodyAsJsonObject());
                        return Uni.createFrom().item(buffer.toJsonObject());
                    } else if (resp.statusCode() == 302) {
                        return Uni.createFrom().failure(OidcCommonUtils.createOidcClientRedirectException(resp));
                    } else {
                        return Uni.createFrom().failure(responseException(requestUri, resp, buffer));
                    }
                });
    }

    private Uni<String> getString(final OidcRequestContextProperties requestProps, String requestUri, HttpResponse<Buffer> resp,
            OidcEndpoint.Type endpoint) {
        return OidcCommonUtils.filterHttpResponse(requestProps, resp, responseFilters, endpoint)
                .flatMap(buffer -> {
                    if (resp.statusCode() == 200) {
                        LOG.debugf("Request succeeded: %s", resp.bodyAsString());
                        return Uni.createFrom().item(buffer.toString());
                    } else if (resp.statusCode() == 302) {
                        return Uni.createFrom().failure(OidcCommonUtils.createOidcClientRedirectException(resp));
                    } else {
                        return Uni.createFrom().failure(responseException(requestUri, resp, buffer));
                    }
                });
    }

    private static OIDCException responseException(String requestUri, HttpResponse<Buffer> resp, Buffer buffer) {
        String errorMessage = buffer == null ? null : buffer.toString();

        if (errorMessage != null && !errorMessage.isEmpty()) {
            LOG.errorf("Request %s has failed: status: %d, error message: %s", requestUri, resp.statusCode(), errorMessage);
            throw new OIDCException(errorMessage);
        } else {
            LOG.errorf("Request %s has failed: status: %d", requestUri, resp.statusCode());
            throw new OIDCException("Error status:" + resp.statusCode());
        }
    }

    @Override
    public void close() {
        client.close();
        if (clientAssertionProvider != null) {
            clientAssertionProvider.close();
        }
    }

    Key getClientJwtKey() {
        return clientJwtKey;
    }

    String getClientSecret() {
        return clientSecret;
    }

    private Uni<HttpRequest<Buffer>> filterHttpRequest(OidcRequestContextProperties requestProps,
            OidcEndpoint.Type endpointType,
            HttpRequest<Buffer> request, Buffer body) {
        return OidcCommonUtils.filterHttpRequest(requestProps, request, body, requestFilters, endpointType);
    }

    private OidcRequestContextProperties getRequestProps(String grantType) {
        return getRequestProps(null, grantType);
    }

    private OidcRequestContextProperties getRequestProps(OidcRequestContextProperties contextProperties) {
        return getRequestProps(contextProperties, null);
    }

    private OidcRequestContextProperties getRequestProps(OidcRequestContextProperties contextProperties, String grantType) {
        if (requestFilters.isEmpty() && responseFilters.isEmpty()) {
            return null;
        }
        Map<String, Object> newProperties = contextProperties == null ? new HashMap<>()
                : new HashMap<>(contextProperties.getAll());
        newProperties.put(OidcUtils.TENANT_ID_ATTRIBUTE, oidcConfig.tenantId().orElse(OidcUtils.DEFAULT_TENANT_ID));
        newProperties.put(OidcConfigurationMetadata.class.getName(), metadata);
        if (grantType != null) {
            newProperties.put(OidcConstants.GRANT_TYPE, grantType);
        }
        return new OidcRequestContextProperties(newProperties);
    }

    Vertx getVertx() {
        return vertx;
    }

    public WebClient getWebClient() {
        return client;
    }

    record UserInfoResponse(String contentType, String data) {
    }

    static boolean isIntrospection(TokenOperation op) {
        return op == TokenOperation.INTROSPECT;
    }

    static Uni<OidcProviderClientImpl> of(WebClient client, Vertx vertx, OidcConfigurationMetadata metadata,
            OidcTenantConfig oidcConfig,
            Map<OidcEndpoint.Type, List<OidcRequestFilter>> requestFilters,
            Map<OidcEndpoint.Type, List<OidcResponseFilter>> responseFilters) {
        return OidcCommonUtils.clientSecret(oidcConfig.credentials())
                .onItem().ifNotNull()
                .transform(clientSecret -> new ClientCredentials(clientSecret,
                        OidcCommonUtils.initClientSecretBasicAuth(oidcConfig, clientSecret)))
                .onItem().ifNull().switchTo(() -> OidcCommonUtils.initClientJwtKey(oidcConfig, true)
                        .onItem().ifNotNull().transform(ClientCredentials::new)
                        .onItem().ifNull()
                        .switchTo(() -> OidcCommonUtils.jwtSecret(oidcConfig.credentials()).map(ClientCredentials::new)))
                .onFailure().invoke(t -> LOG.error("Failed to create OidcProviderClientImpl", t))
                .map(clientCredentials -> new OidcProviderClientImpl(client, vertx, metadata, oidcConfig,
                        clientCredentials, requestFilters, responseFilters));
    }

    String getClientOrJwtSecret() {
        if (clientSecret != null) {
            return clientSecret;
        } else if (jwtSecret != null) {
            LOG.debug("Client secret is not configured, returning configured 'client_jwt_secret' secret");
            return jwtSecret;
        }
        LOG.debug("Client secret and the 'client_jwt_secret' secret are not configured");
        return null;
    }

    private record ClientCredentials(Key clientJwtKey, String clientSecret, String jwtSecret,
            String clientSecretBasicAuthScheme) {

        private ClientCredentials(Key clientJwtKey) {
            this(clientJwtKey, null, null, null);
        }

        private ClientCredentials(String jwtSecret) {
            this(null, null, jwtSecret, null);
        }

        private ClientCredentials(String clientSecret, String clientSecretBasicAuthScheme) {
            this(null, clientSecret, null, clientSecretBasicAuthScheme);
        }
    }
}
