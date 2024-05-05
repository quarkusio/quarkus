package io.quarkus.amazon.lambda.deployment.testing;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.amazon.lambda.deployment.testing.model.InputPerson;

public class GreetingLambda implements RequestHandler<InputPerson, String> {

    @Override
    public String handleRequest(InputPerson input, Context context) {
        return "Hey " + input.getName();
    }
}
