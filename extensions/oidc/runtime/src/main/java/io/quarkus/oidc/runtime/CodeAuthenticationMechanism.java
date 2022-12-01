package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcIdentityProvider.NEW_AUTHENTICATION;
import static io.quarkus.oidc.runtime.OidcIdentityProvider.REFRESH_TOKEN_GRANT_RESPONSE;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.logging.Log;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Authentication;
import io.quarkus.oidc.OidcTenantConfig.Authentication.ResponseMode;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.jwt.algorithm.KeyEncryptionAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class CodeAuthenticationMechanism extends AbstractOidcAuthenticationMechanism {

    static final String AMP = "&";
    static final String EQ = "=";
    static final String UNDERSCORE = "_";
    static final String COOKIE_DELIM = "|";
    static final Pattern COOKIE_PATTERN = Pattern.compile("\\" + COOKIE_DELIM);
    static final String SESSION_MAX_AGE_PARAM = "session-max-age";
    static final String STATE_COOKIE_RESTORE_PATH = "restore-path";
    static final Uni<Void> VOID_UNI = Uni.createFrom().voidItem();
    static final Integer MAX_COOKIE_VALUE_LENGTH = 4096;
    static final String NO_OIDC_COOKIES_AVAILABLE = "no_oidc_cookies";

    private static final String INTERNAL_IDTOKEN_HEADER = "internal";
    private static final Logger LOG = Logger.getLogger(CodeAuthenticationMechanism.class);

    private final BlockingTaskRunner<String> createTokenStateRequestContext = new BlockingTaskRunner<String>();
    private final BlockingTaskRunner<AuthorizationCodeTokens> getTokenStateRequestContext = new BlockingTaskRunner<AuthorizationCodeTokens>();
    private final SecureRandom secureRandom = new SecureRandom();

    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager, OidcTenantConfig oidcTenantConfig) {
        final Cookie sessionCookie = context.request().getCookie(getSessionCookieName(oidcTenantConfig));

        // if the session is already established then try to re-authenticate
        if (sessionCookie != null) {
            LOG.debug("Session cookie is present, starting the reauthentication");
            context.put(OidcUtils.SESSION_COOKIE_NAME, sessionCookie.getName());
            Uni<TenantConfigContext> resolvedContext = resolver.resolveContext(context);
            return resolvedContext.onItem()
                    .transformToUni(new Function<TenantConfigContext, Uni<? extends SecurityIdentity>>() {
                        @Override
                        public Uni<SecurityIdentity> apply(TenantConfigContext tenantContext) {
                            return reAuthenticate(sessionCookie, context, identityProviderManager, tenantContext);
                        }
                    });
        }

        final Cookie stateCookie = context.request().getCookie(getStateCookieName(oidcTenantConfig));

        // if the state cookie is available then try to complete the code flow and start a new session
        if (stateCookie != null) {
            LOG.debug("State cookie is present, processing an expected redirect from the OIDC provider");
            if (ResponseMode.FORM_POST == oidcTenantConfig.authentication.responseMode.orElse(ResponseMode.QUERY)) {
                if (OidcUtils.isFormUrlEncodedRequest(context)) {
                    return OidcUtils.getFormUrlEncodedData(context).onItem()
                            .transformToUni(new Function<MultiMap, Uni<? extends SecurityIdentity>>() {
                                @Override
                                public Uni<? extends SecurityIdentity> apply(MultiMap requestParams) {
                                    return processRedirectFromOidc(context, oidcTenantConfig, identityProviderManager,
                                            stateCookie,
                                            requestParams);
                                }
                            });
                }
                LOG.debug("HTTP POST and " + HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString()
                        + " content type must be used with the form_post response mode");
                return Uni.createFrom().failure(new AuthenticationFailedException());
            } else {
                return processRedirectFromOidc(context, oidcTenantConfig, identityProviderManager, stateCookie,
                        context.queryParams());
            }
        }
        // return an empty identity - this will lead to a challenge redirecting the user to OpenId Connect provider
        // unless it is detected it is a redirect from the provider in which case HTTP 401 will be returned.
        context.put(NO_OIDC_COOKIES_AVAILABLE, Boolean.TRUE);
        return Uni.createFrom().optional(Optional.empty());

    }

    private Uni<SecurityIdentity> processRedirectFromOidc(RoutingContext context, OidcTenantConfig oidcTenantConfig,
            IdentityProviderManager identityProviderManager, Cookie stateCookie, MultiMap requestParams) {
        String[] parsedStateCookieValue = COOKIE_PATTERN.split(stateCookie.getValue());
        OidcUtils.removeCookie(context, oidcTenantConfig, stateCookie.getName());

        if (!isStateValid(requestParams, parsedStateCookieValue[0])) {
            LOG.error("State verification has failed, completing the code flow with HTTP status 401");
            return Uni.createFrom().failure(new AuthenticationCompletionException());
        }

        if (requestParams.contains(OidcConstants.CODE_FLOW_CODE)) {
            LOG.debug("Authorization code is present, completing the code flow");
            Uni<TenantConfigContext> resolvedContext = resolver.resolveContext(context);
            return resolvedContext.onItem()
                    .transformToUni(new Function<TenantConfigContext, Uni<? extends SecurityIdentity>>() {
                        @Override
                        public Uni<SecurityIdentity> apply(TenantConfigContext tenantContext) {
                            return performCodeFlow(identityProviderManager, context, tenantContext, requestParams,
                                    parsedStateCookieValue);
                        }
                    });
        } else if (requestParams.contains(OidcConstants.CODE_FLOW_ERROR)) {
            OidcUtils.removeCookie(context, oidcTenantConfig, stateCookie.getName());
            String error = requestParams.get(OidcConstants.CODE_FLOW_ERROR);
            String errorDescription = requestParams.get(OidcConstants.CODE_FLOW_ERROR_DESCRIPTION);

            LOG.debugf("Authentication has failed, error: %s, description: %s", error, errorDescription);

            if (oidcTenantConfig.authentication.errorPath.isPresent()) {
                Uni<TenantConfigContext> resolvedContext = resolver.resolveContext(context);
                return resolvedContext.onItem()
                        .transformToUni(new Function<TenantConfigContext, Uni<? extends SecurityIdentity>>() {
                            @Override
                            public Uni<SecurityIdentity> apply(TenantConfigContext tenantContext) {
                                URI absoluteUri = URI.create(context.request().absoluteURI());

                                String userQuery = null;

                                // This is an original redirect from IDP, check if the original request path and query need to be restored
                                CodeAuthenticationStateBean stateBean = getCodeAuthenticationBean(parsedStateCookieValue,
                                        tenantContext);
                                if (stateBean != null && stateBean.getRestorePath() != null) {
                                    String restorePath = stateBean.getRestorePath();
                                    int userQueryIndex = restorePath.indexOf("?");
                                    if (userQueryIndex >= 0 && userQueryIndex + 1 < restorePath.length()) {
                                        userQuery = restorePath.substring(userQueryIndex + 1);
                                    }
                                }

                                StringBuilder errorUri = new StringBuilder(buildUri(context,
                                        isForceHttps(oidcTenantConfig),
                                        absoluteUri.getAuthority(),
                                        oidcTenantConfig.authentication.errorPath.get()));
                                errorUri.append('?')
                                        .append(getRequestParametersAsQuery(absoluteUri, requestParams, oidcTenantConfig));
                                if (userQuery != null) {
                                    errorUri.append('&').append(userQuery);
                                }

                                String finalErrorUri = errorUri.toString();
                                LOG.debugf("Error URI: %s", finalErrorUri);
                                return Uni.createFrom().failure(new AuthenticationRedirectException(finalErrorUri));
                            }
                        });
            } else {
                LOG.error(
                        "Authentication has failed but no error handler is found, completing the code flow with HTTP status 401");
                return Uni.createFrom().failure(new AuthenticationCompletionException());
            }
        } else {
            LOG.error("State cookie is present but neither 'code' nor 'error' query parameter is returned");
            return Uni.createFrom().failure(new AuthenticationCompletionException());
        }

    }

    private String getRequestParametersAsQuery(URI requestUri, MultiMap requestParams, OidcTenantConfig oidcConfig) {
        if (ResponseMode.FORM_POST == oidcConfig.authentication.responseMode.orElse(ResponseMode.QUERY)) {
            return OidcCommonUtils.encodeForm(new io.vertx.mutiny.core.MultiMap(requestParams)).toString();
        } else {
            return requestUri.getRawQuery();
        }
    }

    private boolean isStateValid(MultiMap requestParams, String cookieState) {
        List<String> values = requestParams.getAll(OidcConstants.CODE_FLOW_STATE);
        // IDP must return a 'state' query parameter and the value of the state cookie must start with this parameter's value
        if (values.size() != 1) {
            LOG.debug("State parameter can not be empty or multi-valued");
            return false;
        } else if (!cookieState.equals(values.get(0))) {
            LOG.debug("State cookie value does not match the state query parameter value");
            return false;
        }
        return true;
    }

    private Uni<SecurityIdentity> reAuthenticate(Cookie sessionCookie,
            RoutingContext context,
            IdentityProviderManager identityProviderManager,
            TenantConfigContext configContext) {

        context.put(TenantConfigContext.class.getName(), configContext);
        return resolver.getTokenStateManager().getTokens(context, configContext.oidcConfig,
                sessionCookie.getValue(), getTokenStateRequestContext)
                .chain(new Function<AuthorizationCodeTokens, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<? extends SecurityIdentity> apply(AuthorizationCodeTokens session) {
                        context.put(OidcConstants.ACCESS_TOKEN_VALUE, session.getAccessToken());
                        context.put(AuthorizationCodeTokens.class.getName(), session);
                        // Default token state manager may have encrypted ID token when it was saved in a cookie
                        final String currentIdToken = decryptIdTokenIfEncryptedByProvider(configContext, session.getIdToken());
                        return authenticate(identityProviderManager, context,
                                new IdTokenCredential(currentIdToken,
                                        isInternalIdToken(currentIdToken, configContext)))
                                .call(new Function<SecurityIdentity, Uni<?>>() {
                                    @Override
                                    public Uni<Void> apply(SecurityIdentity identity) {
                                        if (isLogout(context, configContext)) {
                                            LOG.debug("Performing an RP initiated logout");
                                            fireEvent(SecurityEvent.Type.OIDC_LOGOUT_RP_INITIATED, identity);
                                            return buildLogoutRedirectUriUni(context, configContext,
                                                    session.getIdToken());
                                        }
                                        if (isBackChannelLogoutPendingAndValid(configContext, identity)
                                                || isFrontChannelLogoutValid(context, configContext,
                                                        identity)) {
                                            return OidcUtils
                                                    .removeSessionCookie(context, configContext.oidcConfig,
                                                            sessionCookie.getName(),
                                                            resolver.getTokenStateManager())
                                                    .map(new Function<Void, Void>() {
                                                        @Override
                                                        public Void apply(Void t) {
                                                            throw new LogoutException();
                                                        }
                                                    });

                                        }
                                        return VOID_UNI;
                                    }
                                }).onFailure()
                                .recoverWithUni(new Function<Throwable, Uni<? extends SecurityIdentity>>() {
                                    @Override
                                    public Uni<? extends SecurityIdentity> apply(Throwable t) {
                                        if (t instanceof AuthenticationRedirectException) {
                                            LOG.debug("Redirecting after the reauthentication");
                                            return Uni.createFrom().failure((AuthenticationRedirectException) t);
                                        }
                                        if (t instanceof LogoutException) {
                                            LOG.debugf("User has been logged out, authentication challenge is required");
                                            return Uni.createFrom().failure(new AuthenticationFailedException(t));
                                        }

                                        if (!(t instanceof TokenAutoRefreshException)) {
                                            boolean expired = (t.getCause() instanceof InvalidJwtException)
                                                    && ((InvalidJwtException) t.getCause())
                                                            .hasErrorCode(ErrorCodes.EXPIRED);

                                            if (!expired) {
                                                LOG.errorf("ID token verification failure: %s", t.getCause());
                                                return Uni.createFrom()
                                                        .failure(new AuthenticationCompletionException(t.getCause()));
                                            }
                                            if (session.getRefreshToken() == null) {
                                                LOG.debug(
                                                        "Token has expired, token refresh is not possible because the refresh token is null");
                                                return Uni.createFrom()
                                                        .failure(new AuthenticationFailedException(t.getCause()));
                                            }
                                            if (!configContext.oidcConfig.token.refreshExpired) {
                                                LOG.debug("Token has expired, token refresh is not allowed");
                                                return Uni.createFrom()
                                                        .failure(new AuthenticationFailedException(t.getCause()));
                                            }
                                            LOG.debug("Token has expired, trying to refresh it");
                                            return refreshSecurityIdentity(configContext,
                                                    currentIdToken,
                                                    session.getRefreshToken(),
                                                    context,
                                                    identityProviderManager, false, null);
                                        } else if (session.getRefreshToken() != null) {
                                            LOG.debug("Token auto-refresh is starting");
                                            return refreshSecurityIdentity(configContext,
                                                    currentIdToken,
                                                    session.getRefreshToken(),
                                                    context,
                                                    identityProviderManager, true,
                                                    ((TokenAutoRefreshException) t).getSecurityIdentity());
                                        } else {
                                            LOG.debug(
                                                    "Token auto-refresh is required it is not possible because the refresh token is null");
                                            // Auto-refreshing is not possible, just continue with the current security identity
                                            return Uni.createFrom().item(((TokenAutoRefreshException) t).getSecurityIdentity());
                                        }
                                    }
                                });
                    }

                });
    }

    private static String decryptIdTokenIfEncryptedByProvider(TenantConfigContext resolvedContext, String token) {
        if ((resolvedContext.provider.tokenDecryptionKey != null || resolvedContext.provider.client.getClientJwtKey() != null)
                && OidcUtils.isEncryptedToken(token)) {
            try {
                return OidcUtils.decryptString(token,
                        resolvedContext.provider.tokenDecryptionKey != null ? resolvedContext.provider.tokenDecryptionKey
                                : resolvedContext.provider.client.getClientJwtKey(),
                        KeyEncryptionAlgorithm.RSA_OAEP);
            } catch (JoseException ex) {
                Log.debugf("Failed to decrypt a token: %s, a token introspection will be attempted instead", ex.getMessage());
            }
        }
        return token;
    }

    private boolean isBackChannelLogoutPendingAndValid(TenantConfigContext configContext, SecurityIdentity identity) {
        TokenVerificationResult backChannelLogoutTokenResult = resolver.getBackChannelLogoutTokens()
                .remove(configContext.oidcConfig.getTenantId().get());
        if (backChannelLogoutTokenResult != null) {
            JsonObject idTokenJson = OidcUtils.decodeJwtContent(((JsonWebToken) (identity.getPrincipal())).getRawToken());
            String idTokenIss = idTokenJson.getString(Claims.iss.name());
            String logoutTokenIss = backChannelLogoutTokenResult.localVerificationResult.getString(Claims.iss.name());
            if (logoutTokenIss != null && !logoutTokenIss.equals(idTokenIss)) {
                LOG.debugf("Logout token issuer does not match the ID token issuer");
                return false;
            }
            String idTokenSub = idTokenJson.getString(Claims.sub.name());
            String logoutTokenSub = backChannelLogoutTokenResult.localVerificationResult.getString(Claims.sub.name());
            if (logoutTokenSub != null && idTokenSub != null && !logoutTokenSub.equals(idTokenSub)) {
                LOG.debugf("Logout token subject does not match the ID token subject");
                return false;
            }
            String idTokenSid = idTokenJson.getString(OidcConstants.ID_TOKEN_SID_CLAIM);
            String logoutTokenSid = backChannelLogoutTokenResult.localVerificationResult
                    .getString(OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM);
            if (logoutTokenSid != null && idTokenSid != null && !logoutTokenSid.equals(idTokenSid)) {
                LOG.debugf("Logout token session id does not match the ID token session id");
                return false;
            }
            LOG.debugf("Frontchannel logout request for the tenant %s has been completed",
                    configContext.oidcConfig.tenantId.get());

            fireEvent(SecurityEvent.Type.OIDC_BACKCHANNEL_LOGOUT_COMPLETED, identity);

            return true;
        }
        return false;
    }

    private boolean isFrontChannelLogoutValid(RoutingContext context, TenantConfigContext configContext,
            SecurityIdentity identity) {
        if (isEqualToRequestPath(configContext.oidcConfig.logout.frontchannel.path, context, configContext)) {
            JsonObject idTokenJson = OidcUtils.decodeJwtContent(((JsonWebToken) (identity.getPrincipal())).getRawToken());

            String idTokenIss = idTokenJson.getString(Claims.iss.name());
            List<String> frontChannelIss = context.queryParam(Claims.iss.name());
            if (frontChannelIss != null && frontChannelIss.size() == 1 && !frontChannelIss.get(0).equals(idTokenIss)) {
                LOG.debugf("Frontchannel issuer parameter does not match the ID token issuer");
                return false;
            }
            String idTokenSid = idTokenJson.getString(OidcConstants.ID_TOKEN_SID_CLAIM);
            List<String> frontChannelSid = context.queryParam(OidcConstants.FRONT_CHANNEL_LOGOUT_SID_PARAM);
            if (frontChannelSid != null && frontChannelSid.size() == 1 && !frontChannelSid.get(0).equals(idTokenSid)) {
                LOG.debugf("Frontchannel session id parameter does not match the ID token session id");
                return false;
            }
            LOG.debugf("Frontchannel logout request for the tenant %s has been completed",
                    configContext.oidcConfig.tenantId.get());
            fireEvent(SecurityEvent.Type.OIDC_FRONTCHANNEL_LOGOUT_COMPLETED, identity);
            return true;
        }
        return false;
    }

    private boolean isInternalIdToken(String idToken, TenantConfigContext configContext) {
        if (!configContext.oidcConfig.authentication.idTokenRequired.orElse(true)) {
            JsonObject headers = OidcUtils.decodeJwtHeaders(idToken);
            if (headers != null) {
                return headers.getBoolean(INTERNAL_IDTOKEN_HEADER, false);
            }
        }
        return false;
    }

    private boolean isIdTokenRequired(TenantConfigContext configContext) {
        return configContext.oidcConfig.authentication.isIdTokenRequired().orElse(true);
    }

    private boolean isJavaScript(RoutingContext context) {
        String value = context.request().getHeader("X-Requested-With");
        return "JavaScript".equals(value) || "XMLHttpRequest".equals(value);
    }

    // This test determines if the default behavior of returning a 302 should go forward
    // The only case that shouldn't return a 302 is if the call is a XHR and the
    // user has set the auto direct application property to false indicating that
    // the client application will manually handle the redirect to account for SPA behavior
    private boolean shouldAutoRedirect(TenantConfigContext configContext, RoutingContext context) {
        return isJavaScript(context) ? configContext.oidcConfig.authentication.javaScriptAutoRedirect : true;
    }

    public Uni<ChallengeData> getChallenge(RoutingContext context) {

        Uni<TenantConfigContext> tenantContext = resolver.resolveContext(context);
        return tenantContext.onItem().transformToUni(new Function<TenantConfigContext, Uni<? extends ChallengeData>>() {
            @Override
            public Uni<ChallengeData> apply(TenantConfigContext tenantContext) {
                return getChallengeInternal(context, tenantContext);
            }
        });
    }

    public Uni<ChallengeData> getChallengeInternal(RoutingContext context, TenantConfigContext configContext) {
        LOG.debug("Starting an authentication challenge");
        return removeSessionCookie(context, configContext.oidcConfig)
                .chain(new Function<Void, Uni<? extends ChallengeData>>() {

                    @Override
                    public Uni<ChallengeData> apply(Void t) {

                        if (context.get(NO_OIDC_COOKIES_AVAILABLE) != null
                                && isRedirectFromProvider(context, configContext)) {
                            LOG.debug(
                                    "The state cookie is missing after the redirect from OpenId Connect Provider, authentication has failed");
                            return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate", "OIDC"));
                        }

                        if (!shouldAutoRedirect(configContext, context)) {
                            // If the client (usually an SPA) wants to handle the redirect manually, then
                            // return status code 499 and WWW-Authenticate header with the 'OIDC' value.
                            return Uni.createFrom().item(new ChallengeData(499, "WWW-Authenticate", "OIDC"));
                        }

                        StringBuilder codeFlowParams = new StringBuilder(168); // experimentally determined to be a good size for preventing resizing and not wasting space

                        // response_type
                        codeFlowParams.append(OidcConstants.CODE_FLOW_RESPONSE_TYPE).append(EQ)
                                .append(OidcConstants.CODE_FLOW_CODE);

                        // response_mode
                        if (ResponseMode.FORM_POST == configContext.oidcConfig.authentication.responseMode
                                .orElse(ResponseMode.QUERY)) {
                            codeFlowParams.append(AMP).append(OidcConstants.CODE_FLOW_RESPONSE_MODE).append(EQ)
                                    .append(configContext.oidcConfig.authentication.responseMode.get().toString()
                                            .toLowerCase());
                        }

                        // client_id
                        codeFlowParams.append(AMP).append(OidcConstants.CLIENT_ID).append(EQ)
                                .append(OidcCommonUtils.urlEncode(configContext.oidcConfig.clientId.get()));

                        // scope
                        List<String> oidcConfigScopes = configContext.oidcConfig.getAuthentication().scopes.isPresent()
                                ? configContext.oidcConfig.getAuthentication().scopes.get()
                                : Collections.emptyList();
                        List<String> scopes = new ArrayList<>(oidcConfigScopes.size() + 1);
                        if (configContext.oidcConfig.getAuthentication().addOpenidScope.orElse(true)) {
                            scopes.add("openid");
                        }
                        scopes.addAll(oidcConfigScopes);
                        codeFlowParams.append(AMP).append(OidcConstants.TOKEN_SCOPE).append(EQ)
                                .append(OidcCommonUtils.urlEncode(String.join(" ", scopes)));

                        MultiMap requestQueryParams = null;
                        if (!configContext.oidcConfig.getAuthentication().forwardParams.isEmpty()) {
                            requestQueryParams = context.queryParams();
                            for (String forwardedParam : configContext.oidcConfig.getAuthentication().forwardParams.get()) {
                                if (requestQueryParams.contains(forwardedParam)) {
                                    for (String requestQueryParamValue : requestQueryParams.getAll(forwardedParam))
                                        codeFlowParams.append(AMP).append(forwardedParam).append(EQ)
                                                .append(OidcCommonUtils.urlEncode(requestQueryParamValue));
                                    requestQueryParams.remove(forwardedParam);
                                }
                            }
                        }

                        // redirect_uri
                        String redirectPath = getRedirectPath(configContext, context);
                        String redirectUriParam = buildUri(context, isForceHttps(configContext.oidcConfig), redirectPath);
                        LOG.debugf("Authentication request redirect_uri parameter: %s", redirectUriParam);

                        codeFlowParams.append(AMP).append(OidcConstants.CODE_FLOW_REDIRECT_URI).append(EQ)
                                .append(OidcCommonUtils.urlEncode(redirectUriParam));

                        // pkce
                        PkceStateBean pkceStateBean = createPkceStateBean(configContext);

                        // state
                        codeFlowParams.append(AMP).append(OidcConstants.CODE_FLOW_STATE).append(EQ)
                                .append(generateCodeFlowState(context, configContext, redirectPath, requestQueryParams,
                                        pkceStateBean != null ? pkceStateBean.getCodeVerifier() : null));

                        if (pkceStateBean != null) {
                            codeFlowParams
                                    .append(AMP).append(OidcConstants.PKCE_CODE_CHALLENGE).append(EQ)
                                    .append(pkceStateBean.getCodeChallenge());
                            codeFlowParams
                                    .append(AMP).append(OidcConstants.PKCE_CODE_CHALLENGE_METHOD).append(EQ)
                                    .append(OidcConstants.PKCE_CODE_CHALLENGE_S256);
                        }

                        // extra redirect parameters, see https://openid.net/specs/openid-connect-core-1_0.html#AuthRequests
                        addExtraParamsToUri(codeFlowParams, configContext.oidcConfig.authentication.getExtraParams());

                        String authorizationURL = configContext.provider.getMetadata().getAuthorizationUri() + "?"
                                + codeFlowParams.toString();

                        LOG.debugf("Code flow redirect to: %s", authorizationURL);

                        return Uni.createFrom().item(new ChallengeData(HttpResponseStatus.FOUND.code(), HttpHeaders.LOCATION,
                                authorizationURL));
                    }

                });
    }

    private boolean isRedirectFromProvider(RoutingContext context, TenantConfigContext configContext) {
        // The referrer check is the best effort at attempting to avoid the redirect loop after
        // the user has authenticated at the OpenId Connect Provider page but the state cookie has been lost
        // during the redirect back to Quarkus.

        String referer = context.request().getHeader(HttpHeaders.REFERER);
        return referer != null && referer.startsWith(configContext.provider.getMetadata().getAuthorizationUri());
    }

    private PkceStateBean createPkceStateBean(TenantConfigContext configContext) {
        if (configContext.oidcConfig.authentication.pkceRequired.orElse(false)) {
            PkceStateBean bean = new PkceStateBean();

            Encoder encoder = Base64.getUrlEncoder().withoutPadding();

            // code verifier
            byte[] codeVerifierBytes = new byte[32];
            secureRandom.nextBytes(codeVerifierBytes);
            String codeVerifier = encoder.encodeToString(codeVerifierBytes);
            bean.setCodeVerifier(codeVerifier);

            // code challenge
            try {
                byte[] codeChallengeBytes = OidcUtils.getSha256Digest(codeVerifier.getBytes(StandardCharsets.ISO_8859_1));
                String codeChallenge = encoder.encodeToString(codeChallengeBytes);
                bean.setCodeChallenge(codeChallenge);
            } catch (Exception ex) {
                LOG.errorf("Code challenge creation failure: %s", ex.getMessage());
                throw new AuthenticationCompletionException(ex);
            }

            return bean;
        }
        return null;
    }

    private Uni<SecurityIdentity> performCodeFlow(IdentityProviderManager identityProviderManager,
            RoutingContext context, TenantConfigContext configContext, MultiMap requestParams,
            String[] parsedStateCookieValue) {

        String userPath = null;
        String userQuery = null;

        // This is an original redirect from IDP, check if the original request path and query need to be restored
        CodeAuthenticationStateBean stateBean = getCodeAuthenticationBean(parsedStateCookieValue, configContext);
        if (stateBean != null && stateBean.getRestorePath() != null) {
            String restorePath = stateBean.getRestorePath();
            int userQueryIndex = restorePath.indexOf("?");
            if (userQueryIndex >= 0) {
                userPath = isRestorePath(configContext.oidcConfig.authentication) ? restorePath.substring(0, userQueryIndex)
                        : null;
                if (userQueryIndex + 1 < restorePath.length()) {
                    userQuery = restorePath.substring(userQueryIndex + 1);
                }
            } else {
                userPath = restorePath;
            }
        }

        final String finalUserPath = userPath;
        final String finalUserQuery = userQuery;

        final String code = requestParams.get(OidcConstants.CODE_FLOW_CODE);
        LOG.debug("Exchanging the authorization code for the tokens");
        Uni<AuthorizationCodeTokens> codeFlowTokensUni = getCodeFlowTokensUni(context, configContext, code,
                stateBean != null ? stateBean.getCodeVerifier() : null);

        return codeFlowTokensUni
                .onItemOrFailure()
                .transformToUni(new BiFunction<AuthorizationCodeTokens, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(final AuthorizationCodeTokens tokens, final Throwable tOuter) {

                        if (tOuter != null) {
                            LOG.errorf("Exception during the code to token exchange: %s", tOuter.getMessage());
                            return Uni.createFrom().failure(new AuthenticationCompletionException(tOuter));
                        }

                        boolean internalIdToken = !isIdTokenRequired(configContext);
                        if (tokens.getIdToken() == null) {
                            if (!internalIdToken) {
                                LOG.errorf("ID token is not available in the authorization code grant response");
                                return Uni.createFrom().failure(new AuthenticationCompletionException());
                            } else {
                                tokens.setIdToken(generateInternalIdToken(configContext.oidcConfig, null, null));
                            }
                        }

                        context.put(NEW_AUTHENTICATION, Boolean.TRUE);
                        context.put(OidcConstants.ACCESS_TOKEN_VALUE, tokens.getAccessToken());
                        context.put(AuthorizationCodeTokens.class.getName(), tokens);

                        // Default token state manager may have encrypted ID token
                        final String idToken = decryptIdTokenIfEncryptedByProvider(configContext, tokens.getIdToken());

                        LOG.debug("Authorization code has been exchanged, verifying ID token");
                        return authenticate(identityProviderManager, context,
                                new IdTokenCredential(idToken, internalIdToken))
                                .call(new Function<SecurityIdentity, Uni<?>>() {
                                    @Override
                                    public Uni<Void> apply(SecurityIdentity identity) {
                                        if (internalIdToken && configContext.oidcConfig.allowUserInfoCache
                                                && configContext.oidcConfig.cacheUserInfoInIdtoken) {
                                            tokens.setIdToken(generateInternalIdToken(configContext.oidcConfig,
                                                    identity.getAttribute(OidcUtils.USER_INFO_ATTRIBUTE), null));
                                        }
                                        return processSuccessfulAuthentication(context, configContext,
                                                tokens, idToken, identity);
                                    }
                                })
                                .map(new Function<SecurityIdentity, SecurityIdentity>() {
                                    @Override
                                    public SecurityIdentity apply(SecurityIdentity identity) {
                                        boolean removeRedirectParams = configContext.oidcConfig.authentication
                                                .isRemoveRedirectParameters();
                                        if (removeRedirectParams || finalUserPath != null
                                                || finalUserQuery != null) {

                                            URI absoluteUri = URI.create(context.request().absoluteURI());

                                            StringBuilder finalUriWithoutQuery = new StringBuilder(buildUri(context,
                                                    isForceHttps(configContext.oidcConfig),
                                                    absoluteUri.getAuthority(),
                                                    (finalUserPath != null ? finalUserPath
                                                            : absoluteUri.getRawPath())));

                                            if (!removeRedirectParams) {
                                                finalUriWithoutQuery.append('?')
                                                        .append(getRequestParametersAsQuery(absoluteUri, requestParams,
                                                                configContext.oidcConfig));
                                            }
                                            if (finalUserQuery != null) {
                                                finalUriWithoutQuery.append(!removeRedirectParams ? "" : "?");
                                                finalUriWithoutQuery.append(finalUserQuery);
                                            }
                                            String finalRedirectUri = finalUriWithoutQuery.toString();
                                            LOG.debugf("Removing code flow redirect parameters, final redirect URI: %s",
                                                    finalRedirectUri);
                                            throw new AuthenticationRedirectException(finalRedirectUri);
                                        } else {
                                            return identity;
                                        }
                                    }
                                }).onFailure().transform(new Function<Throwable, Throwable>() {
                                    @Override
                                    public Throwable apply(Throwable tInner) {
                                        if (tInner instanceof AuthenticationRedirectException) {
                                            LOG.debugf("Starting the final redirect");
                                            return tInner;
                                        }
                                        LOG.errorf("ID token verification has failed: %s", tInner.getMessage());
                                        return new AuthenticationCompletionException(tInner);
                                    }
                                });
                    }

                });

    }

    private CodeAuthenticationStateBean getCodeAuthenticationBean(String[] parsedStateCookieValue,
            TenantConfigContext configContext) {
        if (parsedStateCookieValue.length == 2) {
            CodeAuthenticationStateBean bean = new CodeAuthenticationStateBean();
            if (!configContext.oidcConfig.authentication.pkceRequired.orElse(false)) {
                bean.setRestorePath(parsedStateCookieValue[1]);
                return bean;
            }

            JsonObject json = null;
            try {
                json = OidcUtils.decryptJson(parsedStateCookieValue[1], configContext.getPkceSecretKey());
            } catch (Exception ex) {
                LOG.errorf("State cookie value can not be decrypted for the %s tenant",
                        configContext.oidcConfig.tenantId.get());
                throw new AuthenticationCompletionException(ex);
            }
            bean.setRestorePath(json.getString(STATE_COOKIE_RESTORE_PATH));
            bean.setCodeVerifier(json.getString(OidcConstants.PKCE_CODE_VERIFIER));
            return bean;
        }
        return null;
    }

    private String generateInternalIdToken(OidcTenantConfig oidcConfig, UserInfo userInfo, String currentIdToken) {
        JwtClaimsBuilder builder = Jwt.claims();
        if (currentIdToken != null) {
            AbstractJsonObjectResponse currentIdTokenJson = new AbstractJsonObjectResponse(
                    OidcUtils.decodeJwtContentAsString(currentIdToken)) {
            };
            for (String claim : currentIdTokenJson.getPropertyNames()) {
                // Ignore "iat"(issued at) and "exp"(expiry) claims, new "iat" and "exp" claims will be generated
                if (!claim.equals(Claims.iat.name()) && !claim.equals(Claims.exp.name())) {
                    builder.claim(claim, currentIdTokenJson.get(claim));
                }
            }
        }
        if (userInfo != null) {
            builder.claim(OidcUtils.USER_INFO_ATTRIBUTE, userInfo.getJsonObject());
        }
        return builder.jws().header(INTERNAL_IDTOKEN_HEADER, true)
                .sign(KeyUtils.createSecretKeyFromSecret(OidcCommonUtils.clientSecret(oidcConfig.credentials)));
    }

    private Uni<Void> processSuccessfulAuthentication(RoutingContext context,
            TenantConfigContext configContext,
            AuthorizationCodeTokens tokens,
            String idToken,
            SecurityIdentity securityIdentity) {
        LOG.debug("ID token has been verified, removing the existing session cookie if any and creating a new one");
        return removeSessionCookie(context, configContext.oidcConfig)
                .chain(new Function<Void, Uni<? extends Void>>() {

                    @Override
                    public Uni<? extends Void> apply(Void t) {
                        JsonObject idTokenJson = OidcUtils.decodeJwtContent(idToken);

                        if (!idTokenJson.containsKey("exp") || !idTokenJson.containsKey("iat")) {
                            LOG.error("ID Token is required to contain 'exp' and 'iat' claims");
                            throw new AuthenticationCompletionException();
                        }
                        long maxAge = idTokenJson.getLong("exp") - idTokenJson.getLong("iat");
                        LOG.debugf("ID token is valid for %d seconds", maxAge);
                        if (configContext.oidcConfig.token.lifespanGrace.isPresent()) {
                            maxAge += configContext.oidcConfig.token.lifespanGrace.getAsInt();
                        }
                        if (configContext.oidcConfig.token.refreshExpired) {
                            maxAge += configContext.oidcConfig.authentication.sessionAgeExtension.getSeconds();
                        }
                        final long sessionMaxAge = maxAge;
                        context.put(SESSION_MAX_AGE_PARAM, maxAge);
                        context.put(TenantConfigContext.class.getName(), configContext);
                        // Just in case, remove the stale Back-Channel Logout data if the previous session was not terminated correctly
                        resolver.getBackChannelLogoutTokens().remove(configContext.oidcConfig.tenantId.get());

                        return resolver.getTokenStateManager()
                                .createTokenState(context, configContext.oidcConfig, tokens, createTokenStateRequestContext)
                                .map(new Function<String, Void>() {

                                    @Override
                                    public Void apply(String cookieValue) {
                                        String sessionCookie = createCookie(context, configContext.oidcConfig,
                                                getSessionCookieName(configContext.oidcConfig),
                                                cookieValue, sessionMaxAge).getValue();
                                        if (sessionCookie.length() >= MAX_COOKIE_VALUE_LENGTH) {
                                            LOG.warnf(
                                                    "Session cookie length for the tenant %s is equal or greater than %d bytes."
                                                            + " Browsers may ignore this cookie which will cause a new challenge for the authenticated users."
                                                            + " Recommendations: 1. Set 'quarkus.oidc.token-state-manager.split-tokens=true'"
                                                            + " to have the ID, access and refresh tokens stored in separate cookies."
                                                            + " 2. Set 'quarkus.oidc.token-state-manager.strategy=id-refresh-tokens' if you do not need to use the access token"
                                                            + " as a source of roles or to request UserInfo or propagate it to the downstream services."
                                                            + " 3. Register a custom 'quarkus.oidc.TokenStateManager' CDI bean with the alternative priority set to 1.",
                                                    configContext.oidcConfig.tenantId.get(),
                                                    MAX_COOKIE_VALUE_LENGTH);
                                        }
                                        fireEvent(SecurityEvent.Type.OIDC_LOGIN, securityIdentity);
                                        return null;
                                    }

                                });
                    }

                });

    }

    private void fireEvent(SecurityEvent.Type eventType, SecurityIdentity securityIdentity) {
        if (resolver.isSecurityEventObserved()) {
            resolver.getSecurityEvent().fire(new SecurityEvent(eventType, securityIdentity));
        }
    }

    private String getRedirectPath(TenantConfigContext configContext, RoutingContext context) {
        Authentication auth = configContext.oidcConfig.getAuthentication();
        return auth.getRedirectPath().isPresent() ? auth.getRedirectPath().get() : context.request().path();
    }

    private String generateCodeFlowState(RoutingContext context, TenantConfigContext configContext,
            String redirectPath, MultiMap requestQueryWithoutForwardedParams, String pkceCodeVerifier) {
        String uuid = UUID.randomUUID().toString();
        String cookieValue = uuid;

        boolean restorePath = isRestorePath(configContext.oidcConfig.getAuthentication());
        if (restorePath || pkceCodeVerifier != null) {
            CodeAuthenticationStateBean extraStateValue = new CodeAuthenticationStateBean();
            if (restorePath) {
                String requestQuery = context.request().query();
                String requestPath = !redirectPath.equals(context.request().path()) || requestQuery != null
                        ? context.request().path()
                        : "";
                if (requestQuery != null) {
                    requestPath += "?";
                    if (requestQueryWithoutForwardedParams == null) {
                        requestPath += requestQuery;
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (String requestQueryParam : requestQueryWithoutForwardedParams.names()) {
                            for (String requestQueryParamValue : requestQueryWithoutForwardedParams.getAll(requestQueryParam)) {
                                if (sb.length() > 0) {
                                    sb.append(AMP);
                                }
                                sb.append(requestQueryParam).append(EQ)
                                        .append(OidcCommonUtils.urlEncode(requestQueryParamValue));
                            }
                        }
                        requestPath += sb.toString();
                    }
                }
                if (!requestPath.isEmpty()) {
                    extraStateValue.setRestorePath(requestPath);
                }
            }
            extraStateValue.setCodeVerifier(pkceCodeVerifier);
            if (!extraStateValue.isEmpty()) {
                cookieValue += (COOKIE_DELIM + encodeExtraStateValue(extraStateValue, configContext));
            }
        } else if (context.request().query() != null) {
            CodeAuthenticationStateBean extraStateValue = new CodeAuthenticationStateBean();
            extraStateValue.setRestorePath("?" + context.request().query());
            cookieValue += (COOKIE_DELIM + encodeExtraStateValue(extraStateValue, configContext));
        }
        createCookie(context, configContext.oidcConfig, getStateCookieName(configContext.oidcConfig), cookieValue, 60 * 30);
        return uuid;
    }

    private boolean isRestorePath(Authentication auth) {
        return auth.isRestorePathAfterRedirect() || !auth.redirectPath.isPresent();
    }

    private String encodeExtraStateValue(CodeAuthenticationStateBean extraStateValue, TenantConfigContext configContext) {
        if (extraStateValue.getCodeVerifier() != null) {
            JsonObject json = new JsonObject();
            json.put(OidcConstants.PKCE_CODE_VERIFIER, extraStateValue.getCodeVerifier());
            if (extraStateValue.getRestorePath() != null) {
                json.put(STATE_COOKIE_RESTORE_PATH, extraStateValue.getRestorePath());
            }
            try {
                return OidcUtils.encryptJson(json, configContext.getPkceSecretKey());
            } catch (Exception ex) {
                LOG.errorf("State containing the code verifier can not be encrypted: %s", ex.getMessage());
                throw new AuthenticationCompletionException(ex);
            }
        } else {
            return extraStateValue.getRestorePath();
        }

    }

    private String generatePostLogoutState(RoutingContext context, TenantConfigContext configContext) {
        OidcUtils.removeCookie(context, configContext.oidcConfig, getPostLogoutCookieName(configContext.oidcConfig));
        return createCookie(context, configContext.oidcConfig, getPostLogoutCookieName(configContext.oidcConfig),
                UUID.randomUUID().toString(),
                60 * 30).getValue();
    }

    static ServerCookie createCookie(RoutingContext context, OidcTenantConfig oidcConfig,
            String name, String value, long maxAge) {
        ServerCookie cookie = new CookieImpl(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(oidcConfig.authentication.cookieForceSecure || context.request().isSSL());
        cookie.setMaxAge(maxAge);
        LOG.debugf(name + " cookie 'max-age' parameter is set to %d", maxAge);
        Authentication auth = oidcConfig.getAuthentication();
        OidcUtils.setCookiePath(context, auth, cookie);
        if (auth.cookieDomain.isPresent()) {
            cookie.setDomain(auth.getCookieDomain().get());
        }
        context.response().addCookie(cookie);
        return cookie;
    }

    private String buildUri(RoutingContext context, boolean forceHttps, String path) {
        String authority = URI.create(context.request().absoluteURI()).getAuthority();
        return buildUri(context, forceHttps, authority, path);
    }

    private String buildUri(RoutingContext context, boolean forceHttps, String authority, String path) {
        final String scheme = forceHttps ? "https" : context.request().scheme();
        String forwardedPrefix = "";
        if (resolver.isEnableHttpForwardedPrefix()) {
            String forwardedPrefixHeader = context.request().getHeader("X-Forwarded-Prefix");
            if (forwardedPrefixHeader != null && !forwardedPrefixHeader.equals("/") && !forwardedPrefixHeader.equals("//")) {
                forwardedPrefix = forwardedPrefixHeader;
                if (forwardedPrefix.endsWith("/")) {
                    forwardedPrefix = forwardedPrefix.substring(0, forwardedPrefix.length() - 1);
                }
            }
        }
        return new StringBuilder(scheme).append("://")
                .append(authority)
                .append(forwardedPrefix)
                .append(path)
                .toString();
    }

    private boolean isLogout(RoutingContext context, TenantConfigContext configContext) {
        return isEqualToRequestPath(configContext.oidcConfig.logout.path, context, configContext);
    }

    private boolean isEqualToRequestPath(Optional<String> path, RoutingContext context, TenantConfigContext configContext) {
        if (path.isPresent()) {
            return context.request().path().equals(path.get());
        }

        return false;
    }

    private Uni<SecurityIdentity> refreshSecurityIdentity(TenantConfigContext configContext, String currentIdToken,
            String refreshToken,
            RoutingContext context, IdentityProviderManager identityProviderManager, boolean autoRefresh,
            SecurityIdentity fallback) {

        Uni<AuthorizationCodeTokens> refreshedTokensUni = refreshTokensUni(configContext, currentIdToken, refreshToken,
                autoRefresh);

        return refreshedTokensUni
                .onItemOrFailure()
                .transformToUni(new BiFunction<AuthorizationCodeTokens, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(final AuthorizationCodeTokens tokens, final Throwable t) {
                        if (t != null) {
                            LOG.debugf("ID token refresh has failed: %s", t.getMessage());
                            if (autoRefresh) {
                                LOG.debug("Using the current SecurityIdentity since the ID token is still valid");
                                return Uni.createFrom().item(((TokenAutoRefreshException) t).getSecurityIdentity());
                            } else {
                                return Uni.createFrom().failure(new AuthenticationFailedException(t));
                            }
                        } else {
                            context.put(OidcConstants.ACCESS_TOKEN_VALUE, tokens.getAccessToken());
                            context.put(AuthorizationCodeTokens.class.getName(), tokens);
                            context.put(REFRESH_TOKEN_GRANT_RESPONSE, Boolean.TRUE);

                            // Default token state manager may have encrypted the refreshed ID token
                            final String idToken = decryptIdTokenIfEncryptedByProvider(configContext, tokens.getIdToken());

                            LOG.debug("Verifying the refreshed ID token");
                            return authenticate(identityProviderManager, context,
                                    new IdTokenCredential(idToken,
                                            isInternalIdToken(idToken, configContext)))
                                    .call(new Function<SecurityIdentity, Uni<?>>() {
                                        @Override
                                        public Uni<Void> apply(SecurityIdentity identity) {
                                            // after a successful refresh, rebuild the identity and update the cookie
                                            return processSuccessfulAuthentication(context, configContext,
                                                    tokens, idToken, identity);
                                        }
                                    })
                                    .map(new Function<SecurityIdentity, SecurityIdentity>() {
                                        @Override
                                        public SecurityIdentity apply(SecurityIdentity identity) {
                                            fireEvent(autoRefresh ? SecurityEvent.Type.OIDC_SESSION_REFRESHED
                                                    : SecurityEvent.Type.OIDC_SESSION_EXPIRED_AND_REFRESHED,
                                                    identity);

                                            return identity;
                                        }
                                    }).onFailure().transform(new Function<Throwable, Throwable>() {
                                        @Override
                                        public Throwable apply(Throwable tInner) {
                                            LOG.debugf("Verifying the refreshed ID token failed %s", tInner.getMessage());
                                            return new AuthenticationFailedException(tInner);
                                        }
                                    });
                        }
                    }
                });
    }

    private Uni<AuthorizationCodeTokens> refreshTokensUni(TenantConfigContext configContext,
            String currentIdToken, String refreshToken, boolean autoRefresh) {
        return configContext.provider.refreshTokens(refreshToken).onItem()
                .transform(new Function<AuthorizationCodeTokens, AuthorizationCodeTokens>() {
                    @Override
                    public AuthorizationCodeTokens apply(AuthorizationCodeTokens tokens) {

                        if (tokens.getRefreshToken() == null) {
                            tokens.setRefreshToken(refreshToken);
                        }

                        if (tokens.getIdToken() == null) {
                            if (isIdTokenRequired(configContext)) {
                                if (!autoRefresh) {
                                    LOG.debugf(
                                            "ID token is not returned in the refresh token grant response, re-authentication is required");
                                    throw new AuthenticationFailedException();
                                } else {
                                    // Auto-refresh is triggered while current ID token is still valid, continue using it.
                                    tokens.setIdToken(currentIdToken);
                                }
                            } else {
                                tokens.setIdToken(generateInternalIdToken(configContext.oidcConfig, null, currentIdToken));
                            }
                        }

                        return tokens;
                    }

                });
    }

    private Uni<AuthorizationCodeTokens> getCodeFlowTokensUni(RoutingContext context, TenantConfigContext configContext,
            String code, String codeVerifier) {

        // 'redirect_uri': typically it must match the 'redirect_uri' query parameter which was used during the code request.
        String redirectPath = getRedirectPath(configContext, context);
        String redirectUriParam = buildUri(context, isForceHttps(configContext.oidcConfig), redirectPath);
        LOG.debugf("Token request redirect_uri parameter: %s", redirectUriParam);

        return configContext.provider.getCodeFlowTokens(code, redirectUriParam, codeVerifier);
    }

    private String buildLogoutRedirectUri(TenantConfigContext configContext, String idToken, RoutingContext context) {
        String logoutPath = configContext.provider.getMetadata().getEndSessionUri();
        StringBuilder logoutUri = new StringBuilder(logoutPath);
        if (idToken != null || configContext.oidcConfig.logout.postLogoutPath.isPresent()) {
            logoutUri.append("?");
        }
        if (idToken != null) {
            logoutUri.append(OidcConstants.LOGOUT_ID_TOKEN_HINT).append(EQ).append(idToken);
        }

        if (configContext.oidcConfig.logout.postLogoutPath.isPresent()) {
            logoutUri.append(AMP).append(configContext.oidcConfig.logout.getPostLogoutUriParam()).append(EQ).append(
                    buildUri(context, isForceHttps(configContext.oidcConfig),
                            configContext.oidcConfig.logout.postLogoutPath.get()));
            logoutUri.append(AMP).append(OidcConstants.LOGOUT_STATE).append(EQ)
                    .append(generatePostLogoutState(context, configContext));
        }

        addExtraParamsToUri(logoutUri, configContext.oidcConfig.logout.extraParams);

        return logoutUri.toString();
    }

    private static void addExtraParamsToUri(StringBuilder builder, Map<String, String> extraParams) {
        if (extraParams != null) {
            for (Map.Entry<String, String> entry : extraParams.entrySet()) {
                builder.append(AMP).append(entry.getKey()).append(EQ).append(OidcCommonUtils.urlEncode(entry.getValue()));
            }
        }
    }

    private boolean isForceHttps(OidcTenantConfig oidcConfig) {
        return oidcConfig.authentication.forceRedirectHttpsScheme.orElse(false);
    }

    private Uni<Void> buildLogoutRedirectUriUni(RoutingContext context, TenantConfigContext configContext,
            String idToken) {
        return removeSessionCookie(context, configContext.oidcConfig)
                .map(new Function<Void, Void>() {
                    @Override
                    public Void apply(Void t) {
                        String logoutUri = buildLogoutRedirectUri(configContext, idToken, context);
                        LOG.debugf("Logout uri: %s");
                        throw new AuthenticationRedirectException(logoutUri);
                    }
                });
    }

    private static String getStateCookieName(OidcTenantConfig oidcConfig) {
        return OidcUtils.STATE_COOKIE_NAME + getCookieSuffix(oidcConfig);
    }

    private static String getPostLogoutCookieName(OidcTenantConfig oidcConfig) {
        return OidcUtils.POST_LOGOUT_COOKIE_NAME + getCookieSuffix(oidcConfig);
    }

    private static String getSessionCookieName(OidcTenantConfig oidcConfig) {
        return OidcUtils.SESSION_COOKIE_NAME + getCookieSuffix(oidcConfig);
    }

    private Uni<Void> removeSessionCookie(RoutingContext context, OidcTenantConfig oidcConfig) {
        String cookieName = getSessionCookieName(oidcConfig);
        return OidcUtils.removeSessionCookie(context, oidcConfig, cookieName, resolver.getTokenStateManager());
    }

    static String getCookieSuffix(OidcTenantConfig oidcConfig) {
        String tenantId = oidcConfig.tenantId.get();
        boolean cookieSuffixConfigured = oidcConfig.authentication.cookieSuffix.isPresent();
        String tenantIdSuffix = (cookieSuffixConfigured || !"Default".equals(tenantId)) ? UNDERSCORE + tenantId : "";

        return cookieSuffixConfigured
                ? (tenantIdSuffix + UNDERSCORE + oidcConfig.authentication.cookieSuffix.get())
                : tenantIdSuffix;
    }
}
