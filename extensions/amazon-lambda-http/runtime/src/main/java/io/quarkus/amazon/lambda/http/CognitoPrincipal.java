package io.quarkus.amazon.lambda.http;

import java.security.Principal;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

/**
 * Represents a Cognito JWT used to authenticate request
 *
 * Will only be allocated if requestContext.authorizer.jwt.claims.cognito:username is set
 * in the http event sent by API Gateway
 */
public class CognitoPrincipal implements Principal {
    private APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT jwt;
    private String name;

    public CognitoPrincipal(APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT jwt) {
        this.jwt = jwt;
        this.name = jwt.getClaims().get("cognito:username");
    }

    @Override
    public String getName() {
        return name;
    }

    public APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT getClaims() {
        return jwt;
    }
}
