package io.quarkus.amazon.lambda.http;

import java.security.Principal;

import io.quarkus.amazon.lambda.http.model.ApiGatewayRequestIdentity;

/**
 * Allocated when IAM is used to authenticate user
 *
 * Will only be allocated if requestContext.identity.user is set
 * in the http event sent by API Gateway
 *
 */
public class IAMPrincipal implements Principal {
    private String name;
    private ApiGatewayRequestIdentity iam;

    public IAMPrincipal(ApiGatewayRequestIdentity identity) {
        this.iam = identity;
        this.name = identity.getUser();
    }

    @Override
    public String getName() {
        return name;
    }

    public ApiGatewayRequestIdentity getIam() {
        return iam;
    }
}
