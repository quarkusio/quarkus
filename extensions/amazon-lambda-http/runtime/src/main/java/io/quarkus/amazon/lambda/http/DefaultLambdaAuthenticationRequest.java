package io.quarkus.amazon.lambda.http;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * This will execute if and only if there is no identity after invoking a LambdaAuthenticationRequest
 */
final public class DefaultLambdaAuthenticationRequest extends BaseAuthenticationRequest {
    private APIGatewayV2HTTPEvent event;

    public DefaultLambdaAuthenticationRequest(APIGatewayV2HTTPEvent event) {
        this.event = event;
    }

    public APIGatewayV2HTTPEvent getEvent() {
        return event;
    }
}
