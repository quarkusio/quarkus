package io.quarkus.rest.runtime.core.parameters;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestAsyncResponse;

public class AsyncResponseExtractor implements ParameterExtractor {

    public static final AsyncResponseExtractor INSTANCE = new AsyncResponseExtractor();

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        QuarkusRestAsyncResponse response = new QuarkusRestAsyncResponse(context);
        context.setAsyncResponse(response);
        return response;
    }

}
