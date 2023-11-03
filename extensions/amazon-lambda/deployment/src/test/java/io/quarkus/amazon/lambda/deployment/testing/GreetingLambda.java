package io.quarkus.amazon.lambda.deployment.testing;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class GreetingLambda implements RequestHandler<Person, String> {

    @Override
    public String handleRequest(Person input, Context context) {
        return "Hey " + input.getName();
    }
}
