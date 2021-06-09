package io.quarkus.amazon.lambda.http;

import java.security.Principal;

import io.quarkus.amazon.lambda.http.model.CognitoAuthorizerClaims;

/**
 * Allocated when cognito is used to authenticate user
 *
 * Will only be allocated if requestContext.authorizer.claims.cognito:username is set
 * in the http event sent by API Gateway
 *
 */
public class CognitoPrincipal implements Principal {
    private CognitoAuthorizerClaims claims;
    private String name;

    public CognitoPrincipal(CognitoAuthorizerClaims claims) {
        this.claims = claims;
        this.name = claims.getUsername();
    }

    @Override
    public String getName() {
        return name;
    }

    public CognitoAuthorizerClaims getClaims() {
        return claims;
    }
}
