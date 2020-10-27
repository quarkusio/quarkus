package io.quarkus.rest.server.runtime.core.parameters;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.jaxrs.QuarkusRestAsyncResponse;

public class AsyncResponseExtractor implements ParameterExtractor {

    public static final AsyncResponseExtractor INSTANCE = new AsyncResponseExtractor();

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        QuarkusRestAsyncResponse response = new QuarkusRestAsyncResponse(context);
        context.setAsyncResponse(response);
        return response;
    }

}
