package io.quarkus.oidc.runtime;

import io.vertx.core.json.JsonObject;

/**
 * 
 * This is a temporary holder of both the local and remote introspection results.
 * Currently, verifying the JWT token will indirectly involve the remote introspection call
 * if the local JWK set has no matching key, while the set itself will be refreshed in parallel.
 * In some cases this may also cause 401 - example, not all OIDC providers have the introspection endpoints (Google, Azure, etc)
 * Going forward, the JWT verification should only involve a local verification after running a JWK refresh in the blocking
 * executor.
 */
public class TokenVerificationResult {
    public TokenVerificationResult(JsonObject localVerificationResult, JsonObject introspectionResult) {
        this.localVerificationResult = localVerificationResult;
        this.introspectionResult = introspectionResult;
    }

    JsonObject localVerificationResult;
    JsonObject introspectionResult;
}
