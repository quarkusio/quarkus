package io.quarkus.lambda.sample;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Hello implements RequestHandler<HelloRequest, String> {
    @Override
    public String handleRequest(HelloRequest request, Context context) {
        return String.format("Hello %s %s.", request.firstName, request.lastName);
    }
}
