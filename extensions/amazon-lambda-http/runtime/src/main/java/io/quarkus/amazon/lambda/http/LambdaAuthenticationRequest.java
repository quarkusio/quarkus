package io.quarkus.amazon.lambda.http;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

public class LambdaAuthenticationRequest extends BaseAuthenticationRequest {
    private APIGatewayV2HTTPEvent event;

    public LambdaAuthenticationRequest(APIGatewayV2HTTPEvent event) {
        this.event = event;
    }

    public APIGatewayV2HTTPEvent getEvent() {
        return event;
    }
}
