package io.quarkus.oidc.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfig;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class OidcUtils {
    private OidcUtils() {

    }

    public static List<String> findRoles(String clientId, OidcConfig.Roles rolesConfig, JsonObject json) throws Exception {
        // If the user configured a specific path - check and enforce a claim at this path exists
        if (rolesConfig.getRoleClaimPath().isPresent()) {
            return findClaimWithRoles(rolesConfig, rolesConfig.getRoleClaimPath().get(), json, true);
        }

        // Check 'groups' next
        List<String> groups = findClaimWithRoles(rolesConfig, "groups", json, false);
        if (!groups.isEmpty()) {
            return groups.stream().map(v -> v.toString()).collect(Collectors.toList());
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

    private static List<String> findClaimWithRoles(OidcConfig.Roles rolesConfig, String claimPath,
            JsonObject json, boolean mustExist) {
        Object claimValue = findClaimValue(claimPath, json, claimPath.split("/"), 0, mustExist);

        if (claimValue instanceof JsonArray) {
            return ((JsonArray) claimValue).stream().map(v -> v.toString()).collect(Collectors.toList());
        } else if (claimValue != null) {
            String sep = rolesConfig.getRoleClaimSeparator().isPresent() ? rolesConfig.getRoleClaimSeparator().get() : " ";
            return Arrays.asList(claimValue.toString().split(sep));
        } else {
            return Collections.emptyList();
        }
    }

    private static Object findClaimValue(String claimPath, JsonObject json, String[] pathArray, int step, boolean mustExist) {
        Object claimValue = json.getValue(pathArray[step]);
        if (claimValue == null) {
            if (mustExist) {
                throw new OIDCException(
                        "No claim exists at the path " + claimPath + " at the path segment " + pathArray[step]);
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
}
