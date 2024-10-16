package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.common.runtime.OidcConstants.TOKEN_SCOPE;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Authentication;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.providers.KnownOidcProviders;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.StringPermission;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity.Builder;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.jwt.algorithm.ContentEncryptionAlgorithm;
import io.smallrye.jwt.algorithm.KeyEncryptionAlgorithm;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public final class OidcUtils {
    private static final Logger LOG = Logger.getLogger(OidcUtils.class);

    public static final String STATE_COOKIE_RESTORE_PATH = "restore-path";
    public static final String CONFIG_METADATA_ATTRIBUTE = "configuration-metadata";
    public static final String USER_INFO_ATTRIBUTE = "userinfo";
    public static final String INTROSPECTION_ATTRIBUTE = "introspection";
    public static final String TENANT_ID_ATTRIBUTE = "tenant-id";
    public static final String TENANT_ID_SET_BY_ANNOTATION = "tenant-id-set-by-annotation";
    public static final String TENANT_ID_SET_BY_SESSION_COOKIE = "tenant-id-set-by-session-cookie";
    public static final String TENANT_ID_SET_BY_STATE_COOKIE = "tenant-id-set-by-state-cookie";
    public static final String DEFAULT_TENANT_ID = "Default";
    public static final String SESSION_COOKIE_NAME = "q_session";
    public static final String SESSION_COOKIE_CHUNK_START = "chunk_";
    public static final String SESSION_COOKIE_CHUNK = "_" + SESSION_COOKIE_CHUNK_START;
    public static final String ACCESS_TOKEN_COOKIE_SUFFIX = "_at";
    public static final String REFRESH_TOKEN_COOKIE_SUFFIX = "_rt";
    public static final String SESSION_AT_COOKIE_NAME = SESSION_COOKIE_NAME + ACCESS_TOKEN_COOKIE_SUFFIX;
    public static final String SESSION_RT_COOKIE_NAME = SESSION_COOKIE_NAME + REFRESH_TOKEN_COOKIE_SUFFIX;
    public static final String STATE_COOKIE_NAME = "q_auth";

    // Browsers enforce that the total Set-Cookie expression such as
    // `q_session_tenant-a=<value>,Path=/somepath,Expires=...` does not exceed 4096
    // Setting the max cookie value length to 4056 gives extra 40 bytes to cover for the name, path, expires attributes in most cases
    // and can be tuned further if necessary.
    public static final Integer MAX_COOKIE_VALUE_LENGTH = 4056;
    public static final String POST_LOGOUT_COOKIE_NAME = "q_post_logout";
    public static final String DEFAULT_SCOPE_SEPARATOR = " ";
    public static final String ANNOTATION_BASED_TENANT_RESOLUTION_ENABLED = "io.quarkus.oidc.runtime.select-tenants-with-annotation";
    static final String UNDERSCORE = "_";
    static final String CODE_ACCESS_TOKEN_RESULT = "code_flow_access_token_result";
    static final String CODE_ACCESS_TOKEN_FAILURE = "code_flow_access_token_failure";
    static final String COMMA = ",";
    static final Uni<Void> VOID_UNI = Uni.createFrom().voidItem();
    static final BlockingTaskRunner<Void> deleteTokensRequestContext = new BlockingTaskRunner<Void>();

    /**
     * This pattern uses a positive lookahead to split an expression around the forward slashes
     * ignoring those which are located inside a pair of the double quotes.
     */
    private static final Pattern CLAIM_PATH_PATTERN = Pattern.compile("\\/(?=(?:(?:[^\"]*\"){2})*[^\"]*$)");
    private static final String EXTRACTED_BEARER_TOKEN = "quarkus.oidc.extracted-bearer-token";
    public static final String QUARKUS_IDENTITY_EXPIRE_TIME = "quarkus.identity.expire-time";

    private OidcUtils() {

    }

    public static String getSessionCookie(RoutingContext context, OidcTenantConfig oidcTenantConfig) {
        final Map<String, Cookie> cookies = context.request().cookieMap();
        return getSessionCookie(context.data(), cookies, oidcTenantConfig);
    }

    public static String getSessionCookie(Map<String, Object> context, Map<String, Cookie> cookies,
            OidcTenantConfig oidcTenantConfig) {
        if (cookies.isEmpty()) {
            return null;
        }
        final String sessionCookieName = getSessionCookieName(oidcTenantConfig);

        if (cookies.containsKey(sessionCookieName)) {
            context.put(OidcUtils.SESSION_COOKIE_NAME, List.of(sessionCookieName));
            return cookies.get(sessionCookieName).getValue();
        } else {
            final String sessionChunkPrefix = sessionCookieName + OidcUtils.SESSION_COOKIE_CHUNK;

            SortedMap<String, String> sessionCookies = new TreeMap<>(new Comparator<String>() {

                @Override
                public int compare(String s1, String s2) {
                    // at this point it is guaranteed cookie names end with `chunk_<somenumber>`
                    int lastUnderscoreIndex1 = s1.lastIndexOf(UNDERSCORE);
                    int lastUnderscoreIndex2 = s2.lastIndexOf(UNDERSCORE);
                    Integer pos1 = Integer.valueOf(s1.substring(lastUnderscoreIndex1 + 1));
                    Integer pos2 = Integer.valueOf(s2.substring(lastUnderscoreIndex2 + 1));
                    return pos1.compareTo(pos2);
                }

            });
            for (String cookieName : cookies.keySet()) {
                if (cookieName.startsWith(sessionChunkPrefix)) {
                    sessionCookies.put(cookieName, cookies.get(cookieName).getValue());
                }
            }
            if (!sessionCookies.isEmpty()) {
                context.put(OidcUtils.SESSION_COOKIE_NAME, new ArrayList<String>(sessionCookies.keySet()));

                StringBuilder sessionCookieValue = new StringBuilder();
                for (String value : sessionCookies.values()) {
                    sessionCookieValue.append(value);
                }
                return sessionCookieValue.toString();
            }
        }
        return null;
    }

    public static String getSessionCookieName(OidcTenantConfig oidcConfig) {
        return OidcUtils.SESSION_COOKIE_NAME + getCookieSuffix(oidcConfig);
    }

    public static String getCookieSuffix(OidcTenantConfig oidcConfig) {
        String tenantId = oidcConfig.tenantId.get();
        boolean cookieSuffixConfigured = oidcConfig.authentication.cookieSuffix.isPresent();
        String tenantIdSuffix = (cookieSuffixConfigured || !DEFAULT_TENANT_ID.equals(tenantId)) ? UNDERSCORE + tenantId : "";

        return cookieSuffixConfigured
                ? (tenantIdSuffix + UNDERSCORE + oidcConfig.authentication.cookieSuffix.get())
                : tenantIdSuffix;
    }

    public static boolean isServiceApp(OidcTenantConfig oidcConfig) {
        return ApplicationType.SERVICE.equals(oidcConfig.applicationType.orElse(ApplicationType.SERVICE));
    }

    public static boolean isWebApp(OidcTenantConfig oidcConfig) {
        return ApplicationType.WEB_APP.equals(oidcConfig.applicationType.orElse(ApplicationType.SERVICE));
    }

    public static boolean isEncryptedToken(String token) {
        return new StringTokenizer(token, ".").countTokens() == 5;
    }

    public static boolean isOpaqueToken(String token) {
        return new StringTokenizer(token, ".").countTokens() != 3;
    }

    public static JsonObject decodeJwtContent(String jwt) {
        String encodedContent = getJwtContentPart(jwt);
        if (encodedContent == null) {
            return null;
        }
        return decodeAsJsonObject(encodedContent);
    }

    public static String decodeJwtContentAsString(String jwt) {
        StringTokenizer tokens = new StringTokenizer(jwt, ".");
        // part 1: skip the token headers
        tokens.nextToken();
        if (!tokens.hasMoreTokens()) {
            return null;
        }
        // part 2: token content
        String encodedContent = tokens.nextToken();

        // let's check only 1 more signature part is available
        if (tokens.countTokens() != 1) {
            return null;
        }
        try {
            return base64UrlDecode(encodedContent);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String getJwtContentPart(String jwt) {
        StringTokenizer tokens = new StringTokenizer(jwt, ".");
        // part 1: skip the token headers
        tokens.nextToken();
        if (!tokens.hasMoreTokens()) {
            return null;
        }
        // part 2: token content
        String encodedContent = tokens.nextToken();

        // let's check only 1 more signature part is available
        if (tokens.countTokens() != 1) {
            return null;
        }
        return encodedContent;
    }

    private static JsonObject decodeAsJsonObject(String encodedContent) {
        try {
            return new JsonObject(base64UrlDecode(encodedContent));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String base64UrlDecode(String encodedContent) {
        return new String(Base64.getUrlDecoder().decode(encodedContent), StandardCharsets.UTF_8);
    }

    public static JsonObject decodeJwtHeaders(String jwt) {
        StringTokenizer tokens = new StringTokenizer(jwt, ".");
        return decodeAsJsonObject(tokens.nextToken());
    }

    public static String decodeJwtHeadersAsString(String jwt) {
        StringTokenizer tokens = new StringTokenizer(jwt, ".");
        return base64UrlDecode(tokens.nextToken());
    }

    public static List<String> findRoles(String clientId, OidcTenantConfig.Roles rolesConfig, JsonObject json) {
        // If the user configured specific paths - check and enforce the claims at these paths exist
        if (rolesConfig.getRoleClaimPath().isPresent()) {
            List<String> roles = new LinkedList<>();
            for (String roleClaimPath : rolesConfig.getRoleClaimPath().get()) {
                roles.addAll(findClaimWithRoles(rolesConfig, roleClaimPath.trim(), json));
            }
            return roles;
        }

        // Check 'groups' next
        List<String> groups = findClaimWithRoles(rolesConfig, Claims.groups.name(), json);
        if (!groups.isEmpty()) {
            return groups;
        } else {
            // Finally, check if this token has been issued by Keycloak.
            // Return an empty or populated list of realm and resource access roles
            List<String> allRoles = new LinkedList<>();
            allRoles.addAll(findClaimWithRoles(rolesConfig, "realm_access/roles", json));
            if (clientId != null) {
                allRoles.addAll(findClaimWithRoles(rolesConfig, "resource_access/" + clientId + "/roles", json));
            }

            return allRoles;
        }

    }

    private static List<String> findClaimWithRoles(OidcTenantConfig.Roles rolesConfig, String claimPath,
            JsonObject json) {
        Object claimValue = findClaimValue(claimPath, json, splitClaimPath(claimPath), 0);

        if (claimValue instanceof JsonArray) {
            return convertJsonArrayToList((JsonArray) claimValue);
        } else if (claimValue != null) {
            String sep = rolesConfig.getRoleClaimSeparator().isPresent() ? rolesConfig.getRoleClaimSeparator().get() : " ";
            if (claimValue.toString().isBlank()) {
                return Collections.emptyList();
            }
            return Arrays.asList(claimValue.toString().split(sep));
        } else {
            return Collections.emptyList();
        }
    }

    private static String[] splitClaimPath(String claimPath) {
        return claimPath.indexOf('/') > 0 ? CLAIM_PATH_PATTERN.split(claimPath) : new String[] { claimPath };
    }

    private static Object findClaimValue(String claimPath, JsonObject json, String[] pathArray, int step) {
        Object claimValue = json.getValue(pathArray[step].replace("\"", ""));
        if (claimValue == null) {
            LOG.debugf("No claim exists at the path '%s' at the path segment '%s'", claimPath, pathArray[step]);
        } else if (step + 1 < pathArray.length) {
            if (claimValue instanceof JsonObject) {
                int nextStep = step + 1;
                return findClaimValue(claimPath, (JsonObject) claimValue, pathArray, nextStep);
            } else {
                LOG.debugf("Claim value at the path '%s' is not a json object", claimPath);
            }
        }

        return claimValue;
    }

    private static List<String> convertJsonArrayToList(JsonArray claimValue) {
        List<String> list = new ArrayList<>(claimValue.size());
        for (int i = 0; i < claimValue.size(); i++) {
            String claimValueStr = claimValue.getString(i);
            if (claimValueStr == null || claimValueStr.isBlank()) {
                continue;
            }
            list.add(claimValue.getString(i));
        }
        return list;
    }

    static QuarkusSecurityIdentity validateAndCreateIdentity(Map<String, Object> requestData, TokenCredential credential,
            TenantConfigContext resolvedContext, JsonObject tokenJson, JsonObject rolesJson, UserInfo userInfo,
            TokenIntrospection introspectionResult, TokenAuthenticationRequest request) {

        OidcTenantConfig config = resolvedContext.oidcConfig();
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.addCredential(credential);
        AuthorizationCodeTokens codeTokens = (AuthorizationCodeTokens) requestData.get(AuthorizationCodeTokens.class.getName());
        if (codeTokens != null) {
            RefreshToken refreshTokenCredential = new RefreshToken(codeTokens.getRefreshToken());
            builder.addCredential(refreshTokenCredential);
            builder.addCredential(new AccessTokenCredential(codeTokens.getAccessToken(), refreshTokenCredential));
        }
        JsonWebToken jwtPrincipal;
        try {
            JwtClaims jwtClaims = JwtClaims.parse(tokenJson.encode());
            jwtClaims.setClaim(Claims.raw_token.name(), credential.getToken());
            jwtPrincipal = new OidcJwtCallerPrincipal(jwtClaims, credential,
                    config.token.principalClaim.isPresent() ? config.token.principalClaim.get() : null);
        } catch (InvalidJwtException e) {
            throw new AuthenticationFailedException(e);
        }
        builder.addAttribute(QUARKUS_IDENTITY_EXPIRE_TIME, jwtPrincipal.getExpirationTime());
        builder.setPrincipal(jwtPrincipal);
        var vertxContext = getRoutingContextAttribute(request);
        setRoutingContextAttribute(builder, vertxContext);
        setSecurityIdentityRoles(builder, config, rolesJson);
        setSecurityIdentityPermissions(builder, config, rolesJson);
        setSecurityIdentityUserInfo(builder, userInfo);
        setSecurityIdentityIntrospection(builder, introspectionResult);
        setSecurityIdentityConfigMetadata(builder, resolvedContext);
        setBlockingApiAttribute(builder, vertxContext);
        setTenantIdAttribute(builder, config);
        TokenVerificationResult codeFlowAccessTokenResult = (TokenVerificationResult) requestData.get(CODE_ACCESS_TOKEN_RESULT);
        if (codeFlowAccessTokenResult != null) {
            builder.addAttribute(CODE_ACCESS_TOKEN_RESULT, codeFlowAccessTokenResult);
        }
        return builder.build();
    }

    static void setSecurityIdentityPermissions(QuarkusSecurityIdentity.Builder builder, OidcTenantConfig config,
            JsonObject permissionsJson) {
        addTokenScopesAsPermissions(builder, findClaimWithRoles(config.getRoles(), TOKEN_SCOPE, permissionsJson));
    }

    static void addTokenScopesAsPermissions(Builder builder, Collection<String> scopes) {
        if (!scopes.isEmpty()) {
            builder.addPermissionChecker(new Function<Permission, Uni<Boolean>>() {

                private final Permission[] permissions = transformScopesToPermissions(scopes);

                @Override
                public Uni<Boolean> apply(Permission requiredPermission) {
                    for (Permission possessedPermission : permissions) {
                        if (possessedPermission.implies(requiredPermission)) {
                            // access granted
                            return Uni.createFrom().item(Boolean.TRUE);
                        }
                    }
                    // access denied
                    return Uni.createFrom().item(Boolean.FALSE);
                }
            });
        }
    }

    static Permission[] transformScopesToPermissions(Collection<String> scopes) {
        final Permission[] permissions = new Permission[scopes.size()];
        int i = 0;
        for (String scope : scopes) {
            int semicolonIndex = scope.indexOf(':');
            if (semicolonIndex > 0 && semicolonIndex < scope.length() - 1) {
                permissions[i++] = new StringPermission(scope.substring(0, semicolonIndex),
                        scope.substring(semicolonIndex + 1));
            } else {
                permissions[i++] = new StringPermission(scope);
            }
        }
        return permissions;
    }

    public static void setSecurityIdentityRoles(QuarkusSecurityIdentity.Builder builder, OidcTenantConfig config,
            JsonObject rolesJson) {
        String clientId = config.getClientId().isPresent() ? config.getClientId().get() : null;
        for (String role : findRoles(clientId, config.getRoles(), rolesJson)) {
            builder.addRole(role);
        }
    }

    public static void setBlockingApiAttribute(QuarkusSecurityIdentity.Builder builder, RoutingContext vertxContext) {
        if (vertxContext != null) {
            builder.addAttribute(AuthenticationRequestContext.class.getName(),
                    vertxContext.get(AuthenticationRequestContext.class.getName()));
        }
    }

    public static void setTenantIdAttribute(QuarkusSecurityIdentity.Builder builder, OidcTenantConfig config) {
        builder.addAttribute(TENANT_ID_ATTRIBUTE, config.tenantId.orElse(DEFAULT_TENANT_ID));
    }

    public static void setRoutingContextAttribute(QuarkusSecurityIdentity.Builder builder, RoutingContext routingContext) {
        builder.addAttribute(RoutingContext.class.getName(), routingContext);
    }

    public static void setSecurityIdentityUserInfo(QuarkusSecurityIdentity.Builder builder, UserInfo userInfo) {
        if (userInfo != null) {
            builder.addAttribute(USER_INFO_ATTRIBUTE, userInfo);
        }
    }

    public static void setSecurityIdentityIntrospection(Builder builder, TokenIntrospection introspectionResult) {
        if (introspectionResult != null) {
            builder.addAttribute(INTROSPECTION_ATTRIBUTE, introspectionResult);
        }
    }

    public static void setSecurityIdentityConfigMetadata(QuarkusSecurityIdentity.Builder builder,
            TenantConfigContext resolvedContext) {
        if (resolvedContext.provider().client != null) {
            builder.addAttribute(CONFIG_METADATA_ATTRIBUTE, resolvedContext.provider().client.getMetadata());
        }
    }

    public static void validatePrimaryJwtTokenType(OidcTenantConfig.Token tokenConfig, JsonObject tokenJson) {
        if (tokenJson.containsKey("typ")) {
            String type = tokenJson.getString("typ");
            if (tokenConfig.getTokenType().isPresent() && !tokenConfig.getTokenType().get().equals(type)) {
                throw new OIDCException("Invalid token type");
            } else if ("Refresh".equals(type)) {
                // At least check it is not a refresh token issued by Keycloak
                throw new OIDCException("Refresh token can only be used with the refresh token grant");
            }
        }
    }

    static Uni<Void> removeSessionCookie(RoutingContext context, OidcTenantConfig oidcConfig,
            TokenStateManager tokenStateManager) {
        List<String> cookieNames = context.get(SESSION_COOKIE_NAME);
        if (cookieNames != null) {
            LOG.debugf("Remove session cookie names: %s", cookieNames);
            StringBuilder cookieValue = new StringBuilder();
            for (String cookieName : cookieNames) {
                cookieValue.append(removeCookie(context, oidcConfig, cookieName));
            }
            return tokenStateManager.deleteTokens(context, oidcConfig, cookieValue.toString(),
                    deleteTokensRequestContext);
        } else {
            return VOID_UNI;
        }
    }

    public static String removeCookie(RoutingContext context, OidcTenantConfig oidcConfig, String cookieName) {
        ServerCookie cookie = (ServerCookie) context.cookieMap().get(cookieName);
        String cookieValue = null;
        if (cookie != null) {
            cookieValue = cookie.getValue();
            removeCookie(context, cookie, oidcConfig);
        }
        return cookieValue;
    }

    static void removeCookie(RoutingContext context, ServerCookie cookie, OidcTenantConfig oidcConfig) {
        if (cookie != null) {
            cookie.setValue("");
            cookie.setMaxAge(0);
            Authentication auth = oidcConfig.getAuthentication();
            setCookiePath(context, auth, cookie);
            if (auth.cookieDomain.isPresent()) {
                cookie.setDomain(auth.cookieDomain.get());
            }
        }
    }

    static void setCookiePath(RoutingContext context, Authentication auth, ServerCookie cookie) {
        if (auth.cookiePathHeader.isPresent() && context.request().headers().contains(auth.cookiePathHeader.get())) {
            cookie.setPath(context.request().getHeader(auth.cookiePathHeader.get()));
        } else {
            cookie.setPath(auth.getCookiePath());
        }
    }

    /**
     * Merge the current tenant and well-known OpenId Connect provider configurations.
     * Initialized properties take priority over uninitialized properties.
     *
     * Initialized properties in the current tenant configuration take priority
     * over the same initialized properties in the well-known OpenId Connect provider configuration.
     *
     * Tenant id property of the current tenant must be set before the merge operation.
     *
     * @param tenant current tenant configuration
     * @param provider well-known OpenId Connect provider configuration
     * @return merged configuration
     */
    static OidcTenantConfig mergeTenantConfig(OidcTenantConfig tenant, OidcTenantConfig provider) {
        if (tenant.tenantId.isEmpty()) {
            // OidcRecorder sets it before the merge operation
            throw new IllegalStateException();
        }
        // root properties
        if (tenant.authServerUrl.isEmpty()) {
            tenant.authServerUrl = provider.authServerUrl;
        }
        if (tenant.applicationType.isEmpty()) {
            tenant.applicationType = provider.applicationType;
        }
        if (tenant.discoveryEnabled.isEmpty()) {
            tenant.discoveryEnabled = provider.discoveryEnabled;
        }
        if (tenant.authorizationPath.isEmpty()) {
            tenant.authorizationPath = provider.authorizationPath;
        }
        if (tenant.jwksPath.isEmpty()) {
            tenant.jwksPath = provider.jwksPath;
        }
        if (tenant.tokenPath.isEmpty()) {
            tenant.tokenPath = provider.tokenPath;
        }
        if (tenant.userInfoPath.isEmpty()) {
            tenant.userInfoPath = provider.userInfoPath;
        }

        // authentication
        if (tenant.authentication.idTokenRequired.isEmpty()) {
            tenant.authentication.idTokenRequired = provider.authentication.idTokenRequired;
        }
        if (tenant.authentication.userInfoRequired.isEmpty()) {
            tenant.authentication.userInfoRequired = provider.authentication.userInfoRequired;
        }
        if (tenant.authentication.pkceRequired.isEmpty()) {
            tenant.authentication.pkceRequired = provider.authentication.pkceRequired;
        }
        if (tenant.authentication.scopes.isEmpty()) {
            tenant.authentication.scopes = provider.authentication.scopes;
        }
        if (tenant.authentication.scopeSeparator.isEmpty()) {
            tenant.authentication.scopeSeparator = provider.authentication.scopeSeparator;
        }
        if (tenant.authentication.addOpenidScope.isEmpty()) {
            tenant.authentication.addOpenidScope = provider.authentication.addOpenidScope;
        }
        if (tenant.authentication.forceRedirectHttpsScheme.isEmpty()) {
            tenant.authentication.forceRedirectHttpsScheme = provider.authentication.forceRedirectHttpsScheme;
        }
        if (tenant.authentication.responseMode.isEmpty()) {
            tenant.authentication.responseMode = provider.authentication.responseMode;
        }
        if (tenant.authentication.redirectPath.isEmpty()) {
            tenant.authentication.redirectPath = provider.authentication.redirectPath;
        }

        // credentials
        if (tenant.credentials.clientSecret.method.isEmpty()) {
            tenant.credentials.clientSecret.method = provider.credentials.clientSecret.method;
        }
        if (tenant.credentials.jwt.audience.isEmpty()) {
            tenant.credentials.jwt.audience = provider.credentials.jwt.audience;
        }
        if (tenant.credentials.jwt.signatureAlgorithm.isEmpty()) {
            tenant.credentials.jwt.signatureAlgorithm = provider.credentials.jwt.signatureAlgorithm;
        }

        // token
        if (tenant.token.issuer.isEmpty()) {
            tenant.token.issuer = provider.token.issuer;
        }
        if (tenant.token.principalClaim.isEmpty()) {
            tenant.token.principalClaim = provider.token.principalClaim;
        }
        if (tenant.token.verifyAccessTokenWithUserInfo.isEmpty()) {
            tenant.token.verifyAccessTokenWithUserInfo = provider.token.verifyAccessTokenWithUserInfo;
        }

        return tenant;
    }

    static OidcTenantConfig resolveProviderConfig(OidcTenantConfig oidcTenantConfig) {
        if (oidcTenantConfig != null && oidcTenantConfig.provider.isPresent()) {
            return OidcUtils.mergeTenantConfig(oidcTenantConfig,
                    KnownOidcProviders.provider(oidcTenantConfig.provider.get()));
        } else {
            return oidcTenantConfig;
        }

    }

    public static byte[] getSha256Digest(byte[] value) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(value);
        return sha256.digest();
    }

    public static String encryptJson(JsonObject json, SecretKey key) throws Exception {
        return encryptString(json.encode(), key);
    }

    public static String encryptString(String jweString, SecretKey key) throws Exception {
        return encryptString(jweString, key, KeyEncryptionAlgorithm.A256GCMKW);
    }

    public static String encryptString(String jweString, SecretKey key, KeyEncryptionAlgorithm algorithm) throws Exception {
        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setAlgorithmHeaderValue(algorithm.getAlgorithm());
        jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithm.A256GCM.getAlgorithm());
        jwe.setKey(key);
        jwe.setPlaintext(jweString);
        return jwe.getCompactSerialization();
    }

    public static JsonObject decryptJson(String jweString, Key key) throws Exception {
        return new JsonObject(decryptString(jweString, key));
    }

    public static String decryptString(String jweString, Key key) throws Exception {
        return decryptString(jweString, key, KeyEncryptionAlgorithm.A256GCMKW);
    }

    public static String decryptString(String jweString, Key key, KeyEncryptionAlgorithm algorithm) throws JoseException {
        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setAlgorithmConstraints(new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT,
                algorithm.getAlgorithm()));
        jwe.setKey(key);
        jwe.setCompactSerialization(jweString);
        return jwe.getPlaintextString();
    }

    public static boolean isFormUrlEncodedRequest(RoutingContext context) {
        String contentType = context.request().getHeader("Content-Type");
        return context.request().method() == HttpMethod.POST
                && contentType != null
                && (contentType.equals(HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString())
                        || contentType.startsWith(HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString() + ";"));
    }

    public static Uni<MultiMap> getFormUrlEncodedData(RoutingContext context) {
        context.request().setExpectMultipart(true);
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super MultiMap>>() {
            @Override
            public void accept(UniEmitter<? super MultiMap> t) {
                context.request().endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        t.complete(context.request().formAttributes());
                    }
                });
                context.request().resume();
            }
        });
    }

    public static String encodeScopes(OidcTenantConfig oidcConfig) {
        return OidcCommonUtils.urlEncode(String.join(oidcConfig.authentication.scopeSeparator.orElse(DEFAULT_SCOPE_SEPARATOR),
                getAllScopes(oidcConfig)));
    }

    public static List<String> getAllScopes(OidcTenantConfig oidcConfig) {
        List<String> oidcConfigScopes = oidcConfig.getAuthentication().scopes.isPresent()
                ? oidcConfig.getAuthentication().scopes.get()
                : Collections.emptyList();
        List<String> scopes = new ArrayList<>(oidcConfigScopes.size() + 1);
        if (oidcConfig.getAuthentication().addOpenidScope.orElse(true)) {
            scopes.add(OidcConstants.OPENID_SCOPE);
        }
        scopes.addAll(oidcConfigScopes);
        // Extra scopes if any
        String extraScopeValue = oidcConfig.getAuthentication().getExtraParams()
                .get(OidcConstants.TOKEN_SCOPE);
        if (extraScopeValue != null) {
            String[] extraScopes = extraScopeValue.split(COMMA);
            scopes.addAll(List.of(extraScopes));
        }

        return scopes;
    }

    public static boolean isSessionCookie(String cookieName) {
        return cookieName.startsWith(SESSION_COOKIE_NAME)
                && !cookieName.regionMatches(SESSION_COOKIE_NAME.length(), ACCESS_TOKEN_COOKIE_SUFFIX, 0, 3)
                && !cookieName.regionMatches(SESSION_COOKIE_NAME.length(), REFRESH_TOKEN_COOKIE_SUFFIX, 0, 3);
    }

    static String extractBearerToken(RoutingContext context, OidcTenantConfig oidcConfig) {
        if (context.get(EXTRACTED_BEARER_TOKEN) != null) {
            return context.get(EXTRACTED_BEARER_TOKEN);
        }
        final HttpServerRequest request = context.request();
        String header = oidcConfig.token.header.isPresent() ? oidcConfig.token.header.get()
                : HttpHeaders.AUTHORIZATION.toString();
        LOG.debugf("Looking for a token in the %s header", header);
        final String headerValue = request.headers().get(header);

        if (headerValue == null) {
            return null;
        }

        int idx = headerValue.indexOf(' ');
        final String scheme = idx > 0 ? headerValue.substring(0, idx) : null;

        if (scheme != null) {
            LOG.debugf("Authorization scheme: %s", scheme);
        }

        if (scheme == null && !header.equalsIgnoreCase(HttpHeaders.AUTHORIZATION.toString())) {
            return headerValue;
        }

        if (!oidcConfig.token.authorizationScheme.equalsIgnoreCase(scheme)) {
            return null;
        }

        return headerValue.substring(idx + 1);
    }

    static void storeExtractedBearerToken(RoutingContext context, String token) {
        context.put(EXTRACTED_BEARER_TOKEN, token);
    }

    public static String getTenantIdFromCookie(String cookiePrefix, String cookieName, boolean sessionCookie) {
        // It has already been checked the cookieName starts with the cookiePrefix
        if (cookieName.length() == cookiePrefix.length()) {
            return OidcUtils.DEFAULT_TENANT_ID;
        } else {
            String suffix = cookieName.substring(cookiePrefix.length() + 1);

            if (sessionCookie && suffix.startsWith(OidcUtils.SESSION_COOKIE_CHUNK_START)) {
                return OidcUtils.DEFAULT_TENANT_ID;
            } else {
                // It can be either a tenant_id, or a tenant_id and cookie suffix property, example, q_session_github or q_session_github_test
                // or it can be a session cookie chunk like q_session_chunk_1 in which case the suffix will be chunk_1
                int index = suffix.indexOf("_", 0);
                return index == -1 ? suffix : suffix.substring(0, index);
            }
        }
    }

    public static boolean cacheUserInfoInIdToken(DefaultTenantConfigResolver resolver, OidcTenantConfig oidcConfig) {

        if (resolver.getUserInfoCache() != null && oidcConfig.allowUserInfoCache) {
            return false;
        }
        if (oidcConfig.cacheUserInfoInIdtoken.isPresent()) {
            return oidcConfig.cacheUserInfoInIdtoken.get();
        }
        return resolver.getTokenStateManager() instanceof DefaultTokenStateManager
                && oidcConfig.tokenStateManager.encryptionRequired;
    }

    public static ServerCookie createCookie(RoutingContext context, OidcTenantConfig oidcConfig,
            String name, String value, long maxAge) {
        ServerCookie cookie = new CookieImpl(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(oidcConfig.authentication.cookieForceSecure || context.request().isSSL());
        cookie.setMaxAge(maxAge);
        LOG.debugf(name + " cookie 'max-age' parameter is set to %d", maxAge);
        Authentication auth = oidcConfig.getAuthentication();
        OidcUtils.setCookiePath(context, oidcConfig.getAuthentication(), cookie);
        if (auth.cookieDomain.isPresent()) {
            cookie.setDomain(auth.getCookieDomain().get());
        }
        context.response().addCookie(cookie);
        return cookie;
    }

    public static SecretKey createSecretKeyFromDigest(byte[] secretBytes) {
        try {
            return new SecretKeySpec(getSha256Digest(secretBytes), "AES");
        } catch (Exception ex) {
            throw new OIDCException(ex);
        }
    }

    public static <T extends TokenCredential> T getTokenCredential(SecurityIdentity identity, Class<T> type) {
        T credential = identity.getCredential(type);
        if (credential == null) {
            Map<String, SecurityIdentity> identities = HttpSecurityUtils.getSecurityIdentities(identity);
            if (identities != null) {
                for (String scheme : identities.keySet()) {
                    if (scheme.equalsIgnoreCase(OidcConstants.BEARER_SCHEME)) {
                        return identities.get(scheme).getCredential(type);
                    }
                }
            }
        }
        return credential;
    }

    public static <T> T getAttribute(SecurityIdentity identity, String name) {
        T attribute = identity.getAttribute(name);
        if (attribute == null) {
            Map<String, SecurityIdentity> identities = HttpSecurityUtils.getSecurityIdentities(identity);
            if (identities != null) {
                for (String scheme : identities.keySet()) {
                    if (scheme.equalsIgnoreCase(OidcConstants.BEARER_SCHEME)) {
                        return identities.get(scheme).getAttribute(name);
                    }
                }
            }
        }
        return attribute;
    }

    public static boolean isJwtTokenExpired(String token) {
        if (!isOpaqueToken(token)) {
            JsonObject claims = decodeJwtContent(token);
            Long expiresAt = getJwtExpiresAtClaim(claims);
            if (expiresAt == null) {
                return false;
            }
            final long nowSecs = System.currentTimeMillis() / 1000;
            return nowSecs > expiresAt;
        }
        return false;
    }

    private static Long getJwtExpiresAtClaim(JsonObject claims) {
        if (claims == null || !claims.containsKey(Claims.exp.name())) {
            return null;
        }
        try {
            return claims.getLong(Claims.exp.name());
        } catch (IllegalArgumentException ex) {
            LOG.debug("Refresh JWT expiry claim can not be converted to Long");
            return null;
        }
    }
}
