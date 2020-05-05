package io.quarkus.oidc.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class OidcUtils {
    /**
     * This pattern uses a positive lookahead to split an expression around the forward slashes
     * ignoring those which are located inside a pair of the double quotes.
     */
    private static final Pattern CLAIM_PATH_PATTERN = Pattern.compile("\\/(?=(?:(?:[^\"]*\"){2})*[^\"]*$)");

    private OidcUtils() {

    }

    public static boolean validateClaims(OidcTenantConfig.Token tokenConfig, JsonObject json) {
        if (tokenConfig.issuer.isPresent()) {
            String issuer = json.getString(Claims.iss.name());
            if (!tokenConfig.issuer.get().equals(issuer)) {
                throw new OIDCException("Invalid issuer");
            }
        }
        if (tokenConfig.audience.isPresent()) {
            Object claimValue = json.getValue(Claims.aud.name());
            List<String> audience = Collections.emptyList();
            if (claimValue instanceof JsonArray) {
                audience = convertJsonArrayToList((JsonArray) claimValue);
            } else if (claimValue != null) {
                audience = Arrays.asList((String) claimValue);
            }
            if (!audience.containsAll(tokenConfig.audience.get())) {
                throw new OIDCException("Invalid audience");
            }
        }
        return true;
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

    static QuarkusSecurityIdentity validateAndCreateIdentity(TokenCredential credential,
            OidcTenantConfig config, JsonObject tokenJson) {
        try {
            OidcUtils.validateClaims(config.getToken(), tokenJson);
        } catch (OIDCException e) {
            throw new AuthenticationFailedException(e);
        }

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.addCredential(credential);

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
        try {
            String clientId = config.getClientId().isPresent() ? config.getClientId().get() : null;
            for (String role : OidcUtils.findRoles(clientId, config.getRoles(), tokenJson)) {
                builder.addRole(role);
            }
        } catch (Exception e) {
            throw new ForbiddenException(e);
        }
        return builder.build();
    }
}
