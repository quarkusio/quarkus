package io.quarkus.amazon.lambda.http;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * This will execute if and only if there is no identity after invoking a LambdaAuthenticationRequest
 */
final public class DefaultLambdaAuthenticationRequest extends BaseAuthenticationRequest {
    private AwsProxyRequest event;

    public DefaultLambdaAuthenticationRequest(AwsProxyRequest event) {
        this.event = event;
    }

    public AwsProxyRequest getEvent() {
        return event;
    }
}
