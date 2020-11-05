package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestAsyncResponse;

public class AsyncResponseExtractor implements ParameterExtractor {

    public static final AsyncResponseExtractor INSTANCE = new AsyncResponseExtractor();

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        QuarkusRestAsyncResponse response = new QuarkusRestAsyncResponse(context);
        context.setAsyncResponse(response);
        return response;
    }

}
