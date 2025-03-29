package io.quarkus.it.amazon.lambda;

import jakarta.inject.Inject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class NestedLambda implements RequestHandler<InputObject, OutputObject> {

    @Inject
    ProcessingService service;

    @Count
    @Override
    public OutputObject handleRequest(InputObject input, Context context) {
        return service.process(input).setRequestId(context.getAwsRequestId());
    }

}
