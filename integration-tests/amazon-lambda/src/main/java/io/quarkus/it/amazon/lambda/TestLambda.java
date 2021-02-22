package io.quarkus.it.amazon.lambda;

import javax.inject.Inject;
import javax.inject.Named;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

@Named("test")
public class TestLambda implements RequestHandler<InputObject, OutputObject> {

    @Inject
    ProcessingService service;

    @Count
    @Override
    public OutputObject handleRequest(InputObject input, Context context) {
        return service.proces(input).setRequestId(context.getAwsRequestId());
    }
}
