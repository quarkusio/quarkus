package io.quarkus.oidc;

import javax.json.JsonObject;

import io.quarkus.oidc.runtime.AbstractJsonObjectResponse;

/**
 * Represents a token introspection result
 *
 */
public class TokenIntrospection extends AbstractJsonObjectResponse {

    public TokenIntrospection() {
    }

    public TokenIntrospection(String introspectionJson) {
        super(introspectionJson);
    }

    public TokenIntrospection(JsonObject json) {
        super(json);
    }
}
