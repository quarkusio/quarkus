package io.quarkus.amazon.lambda.http;

import java.security.Principal;
import java.util.Map;

/**
 * Represents a custom principal sent by API Gateway i.e. a Lambda authorizer
 *
 * Will only be allocated if requestContext.authorizer.lambda.principalId is set
 * in the http event sent by API Gateway
 *
 */
public class CustomPrincipal implements Principal {
    private String name;
    private Map<String, Object> claims;

    public CustomPrincipal(String name, Map<String, Object> claims) {
        this.claims = claims;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }
}
