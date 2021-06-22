package io.quarkus.amazon.lambda.http;

import java.security.Principal;
import java.util.Map;

/**
 * Allocated when a custom authorizer (i.e. Lambda) is used to authenticate user
 *
 * Will only be allocated if requestContext.authorizer.principalId is set
 * in the http event sent by API Gateway
 *
 */
public class CustomPrincipal implements Principal {
    private String name;
    private Map<String, String> claims;

    public CustomPrincipal(String name, Map<String, String> claims) {
        this.claims = claims;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public Map<String, String> getClaims() {
        return claims;
    }
}
