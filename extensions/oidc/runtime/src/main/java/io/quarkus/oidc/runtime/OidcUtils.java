package io.quarkus.oidc.runtime;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity.Builder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public final class OidcUtils {
    public static final String CONFIG_METADATA_ATTRIBUTE = "configuration-metadata";
    public static final String USER_INFO_ATTRIBUTE = "userinfo";
    public static final String INTROSPECTION_ATTRIBUTE = "introspection";
    public static final String TENANT_ID_ATTRIBUTE = "tenant-id";
    /**
     * This pattern uses a positive lookahead to split an expression around the forward slashes
     * ignoring those which are located inside a pair of the double quotes.
     */
    private static final Pattern CLAIM_PATH_PATTERN = Pattern.compile("\\/(?=(?:(?:[^\"]*\"){2})*[^\"]*$)");

    private OidcUtils() {

    }

    public static boolean isOpaqueToken(String token) {
        return new StringTokenizer(token, ".").countTokens() != 3;
    }

    public static JsonObject decodeJwtContent(String jwt) {
        StringTokenizer tokens = new StringTokenizer(jwt, ".");
        // part 1: skip the token headers
        tokens.nextToken();
        if (!tokens.hasMoreTokens()) {
            return null;
        }
        // part 2: token content
        String encodedContent = tokens.nextToken();

        // lets check only 1 more signature part is available
        if (tokens.countTokens() != 1) {
            return null;
        }
        try {
            return new JsonObject(new String(Base64.getUrlDecoder().decode(encodedContent), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static List<String> findRoles(String clientId, OidcTenantConfig.Roles rolesConfig, JsonObject json) {
        // If the user configured a specific path - check and enforce a claim at this path exists
        if (rolesConfig.getRoleClaimPath().isPresent()) {
            return findClaimWithRoles(rolesConfig, rolesConfig.getRoleClaimPath().get(), json, true);
        }

        // Check 'groups' next
        List<String> groups = findClaimWithRoles(rolesConfig, Claims.groups.name(), json, false);
        if (!groups.isEmpty()) {
            return groups;
        } else {
            // Finally, check if this token has been issued by Keycloak.
            // Return an empty or populated list of realm and resource access roles
            List<String> allRoles = new LinkedList<>();
            allRoles.addAll(findClaimWithRoles(rolesConfig, "realm_access/roles", json, false));
            if (clientId != null) {
                allRoles.addAll(findClaimWithRoles(rolesConfig, "resource_access/" + clientId + "/roles", json, false));
            }

            return allRoles;
        }

    }

    private static List<String> findClaimWithRoles(OidcTenantConfig.Roles rolesConfig, String claimPath,
            JsonObject json, boolean mustExist) {
        Object claimValue = findClaimValue(claimPath, json, splitClaimPath(claimPath), 0, mustExist);

        if (claimValue instanceof JsonArray) {
            return convertJsonArrayToList((JsonArray) claimValue);
        } else if (claimValue != null) {
            String sep = rolesConfig.getRoleClaimSeparator().isPresent() ? rolesConfig.getRoleClaimSeparator().get() : " ";
            return Arrays.asList(claimValue.toString().split(sep));
        } else {
            return Collections.emptyList();
        }
    }

    private static String[] splitClaimPath(String claimPath) {
        return claimPath.indexOf('/') > 0 ? CLAIM_PATH_PATTERN.split(claimPath) : new String[] { claimPath };
    }

    private static Object findClaimValue(String claimPath, JsonObject json, String[] pathArray, int step, boolean mustExist) {
        Object claimValue = json.getValue(pathArray[step].replace("\"", ""));
        if (claimValue == null) {
            if (mustExist) {
                throw new OIDCException("No claim exists at the path " + claimPath + " at the path segment " + pathArray[step]);
            }
        } else if (step + 1 < pathArray.length) {
            if (claimValue instanceof JsonObject) {
                int nextStep = step + 1;
                return findClaimValue(claimPath, (JsonObject) claimValue, pathArray, nextStep, mustExist);
            } else {
                throw new OIDCException("Claim value at the path " + claimPath + " is not a json object");
            }
        }

        return claimValue;
    }

    private static List<String> convertJsonArrayToList(JsonArray claimValue) {
        List<String> list = new ArrayList<>(claimValue.size());
        for (int i = 0; i < claimValue.size(); i++) {
            list.add(claimValue.getString(i));
        }
        return list;
    }

    static QuarkusSecurityIdentity validateAndCreateIdentity(
            RoutingContext vertxContext, TokenCredential credential,
            TenantConfigContext resolvedContext, JsonObject tokenJson, JsonObject rolesJson, UserInfo userInfo) {

        OidcTenantConfig config = resolvedContext.oidcConfig;
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.addCredential(credential);
        AuthorizationCodeTokens codeTokens = vertxContext != null ? vertxContext.get(AuthorizationCodeTokens.class.getName())
                : null;
        if (codeTokens != null) {
            RefreshToken refreshTokenCredential = new RefreshToken(codeTokens.getRefreshToken());
            builder.addCredential(refreshTokenCredential);
            builder.addCredential(new AccessTokenCredential(codeTokens.getAccessToken(), refreshTokenCredential, vertxContext));
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
        builder.setPrincipal(jwtPrincipal);
        setSecurityIdentityRoles(builder, config, rolesJson);
        setSecurityIdentityUserInfo(builder, userInfo);
        setSecurityIdentityConfigMetadata(builder, resolvedContext);
        setBlockinApiAttribute(builder, vertxContext);
        setTenantIdAttribute(builder, config);
        return builder.build();
    }

    public static void setSecurityIdentityRoles(QuarkusSecurityIdentity.Builder builder, OidcTenantConfig config,
            JsonObject rolesJson) {
        try {
            String clientId = config.getClientId().isPresent() ? config.getClientId().get() : null;
            for (String role : findRoles(clientId, config.getRoles(), rolesJson)) {
                builder.addRole(role);
            }
        } catch (Exception e) {
            throw new ForbiddenException(e);
        }
    }

    public static void setBlockinApiAttribute(QuarkusSecurityIdentity.Builder builder, RoutingContext vertxContext) {
        if (vertxContext != null) {
            builder.addAttribute(AuthenticationRequestContext.class.getName(),
                    vertxContext.get(AuthenticationRequestContext.class.getName()));
        }
    }

    public static void setTenantIdAttribute(QuarkusSecurityIdentity.Builder builder, OidcTenantConfig config) {
        builder.addAttribute(TENANT_ID_ATTRIBUTE, config.tenantId.orElse("Default"));
    }

    public static void setSecurityIdentityUserInfo(QuarkusSecurityIdentity.Builder builder, UserInfo userInfo) {
        if (userInfo != null) {
            builder.addAttribute(USER_INFO_ATTRIBUTE, userInfo);
        }
    }

    public static void setSecurityIdentityIntrospecton(Builder builder, TokenIntrospection introspectionResult) {
        builder.addAttribute(INTROSPECTION_ATTRIBUTE, introspectionResult);
    }

    public static void setSecurityIdentityConfigMetadata(QuarkusSecurityIdentity.Builder builder,
            TenantConfigContext resolvedContext) {
        if (resolvedContext.provider.client != null) {
            builder.addAttribute(CONFIG_METADATA_ATTRIBUTE, resolvedContext.provider.client.getMetadata());
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
}
