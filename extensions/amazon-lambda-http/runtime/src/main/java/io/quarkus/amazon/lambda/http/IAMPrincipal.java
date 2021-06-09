package io.quarkus.amazon.lambda.http;

import java.security.Principal;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

/**
 * Used if IAM is used for authentication.
 *
 * Will only be allocated if requestContext.authorizer.iam.userId is set
 * in the http event sent by API Gateway
 */
public class IAMPrincipal implements Principal {
    private String name;
    private APIGatewayV2HTTPEvent.RequestContext.IAM iam;

    public IAMPrincipal(APIGatewayV2HTTPEvent.RequestContext.IAM iam) {
        this.iam = iam;
        this.name = iam.getUserId();
    }

    @Override
    public String getName() {
        return name;
    }

    public APIGatewayV2HTTPEvent.RequestContext.IAM getIam() {
        return iam;
    }
}
