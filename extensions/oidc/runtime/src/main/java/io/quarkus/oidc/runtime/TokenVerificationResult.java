package io.quarkus.oidc.runtime;

import io.vertx.core.json.JsonObject;

public class TokenVerificationResult {
    public TokenVerificationResult(JsonObject localVerificationResult, JsonObject introspectionResult) {
        this.localVerificationResult = localVerificationResult;
        this.introspectionResult = introspectionResult;
    }

    JsonObject localVerificationResult;
    JsonObject introspectionResult;
}
