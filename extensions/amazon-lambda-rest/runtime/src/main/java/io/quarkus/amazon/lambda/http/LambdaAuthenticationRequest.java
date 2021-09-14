package io.quarkus.amazon.lambda.http;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.security.identity.request.BaseAuthenticationRequest;

public class LambdaAuthenticationRequest extends BaseAuthenticationRequest {
    private AwsProxyRequest event;

    public LambdaAuthenticationRequest(AwsProxyRequest event) {
        this.event = event;
    }

    public AwsProxyRequest getEvent() {
        return event;
    }
}
