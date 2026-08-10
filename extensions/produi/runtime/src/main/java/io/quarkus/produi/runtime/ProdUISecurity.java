package io.quarkus.produi.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the HTTP security configuration that restricts Prod UI to a set of roles.
 * <p>
 * Prod UI is served in production, so when {@code quarkus.prod-ui.roles-allowed} is set it must require an authenticated
 * user with one of those roles on <em>every</em> route - including the JSON-RPC websocket that carries all data.
 * Rather than ship its own security runtime, Prod UI contributes standard vert.x-http auth config as build-time
 * defaults: a named role policy plus a permission binding the policy to the Prod UI paths. Authentication itself stays
 * delegated to the interface Prod UI is served on (configure it with {@code quarkus.management.auth.*} or
 * {@code quarkus.http.auth.*}, e.g. basic, OIDC or mTLS).
 */
public final class ProdUISecurity {

    /** Name of the generated permission and role policy that guard Prod UI. */
    public static final String POLICY_NAME = "quarkus-prod-ui";

    private ProdUISecurity() {
    }

    /**
     * Builds the vert.x-http auth config defaults that restrict {@code path} (and everything under it, i.e. the static
     * assets and the JSON-RPC websocket) to {@code rolesAllowed}. Returns an empty map when no roles are configured, so
     * Prod UI adds no authorization of its own.
     *
     * @param authConfigPrefix the auth config root to write under, e.g. {@code quarkus.management.auth} when Prod UI is
     *        served on the management interface, or {@code quarkus.http.auth} when it is on the main interface
     * @param path the resolved absolute path of Prod UI, e.g. {@code /q/prod-ui}
     * @param rolesAllowed the roles permitted to access Prod UI
     */
    public static Map<String, String> authConfigDefaults(String authConfigPrefix, String path, List<String> rolesAllowed) {
        if (rolesAllowed == null || rolesAllowed.isEmpty()) {
            return Map.of();
        }
        String base = stripTrailingSlash(path);
        Map<String, String> config = new LinkedHashMap<>();
        // Match the exact base path and everything below it (static assets + the json-rpc-ws upgrade).
        config.put(authConfigPrefix + ".permission." + POLICY_NAME + ".paths", base + "," + base + "/*");
        config.put(authConfigPrefix + ".permission." + POLICY_NAME + ".policy", POLICY_NAME);
        config.put(authConfigPrefix + ".policy." + POLICY_NAME + ".roles-allowed", String.join(",", rolesAllowed));
        return config;
    }

    private static String stripTrailingSlash(String path) {
        if (path != null && path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}
