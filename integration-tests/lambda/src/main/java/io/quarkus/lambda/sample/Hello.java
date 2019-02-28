package io.quarkus.lambda.sample;

import javax.inject.Inject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Hello implements RequestHandler<HelloRequest, String> {

    @Inject
    HelloGreeter greeter;

    @Override
    public String handleRequest(HelloRequest request, Context context) {
        return greeter.greet(request.firstName, request.lastName);
    }
}
