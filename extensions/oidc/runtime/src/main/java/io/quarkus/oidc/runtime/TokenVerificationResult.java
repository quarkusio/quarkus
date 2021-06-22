package io.quarkus.oidc.runtime;

import io.quarkus.oidc.TokenIntrospection;
import io.vertx.core.json.JsonObject;

public class TokenVerificationResult {
    JsonObject localVerificationResult;
    TokenIntrospection introspectionResult;

    public TokenVerificationResult(JsonObject localVerificationResult, TokenIntrospection introspectionResult) {
        this.localVerificationResult = localVerificationResult;
        this.introspectionResult = introspectionResult;
    }
}
