package io.quarkus.oidc.runtime;

import io.quarkus.oidc.TokenIntrospection;
import io.vertx.core.json.JsonObject;

public record TokenVerificationResult(JsonObject localVerificationResult, TokenIntrospection introspectionResult) {
}
