package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.AsyncResponseImpl;

public class AsyncResponseExtractor implements ParameterExtractor {

    public static final AsyncResponseExtractor INSTANCE = new AsyncResponseExtractor();

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        AsyncResponseImpl response = new AsyncResponseImpl(context);
        context.setAsyncResponse(response);
        return response;
    }

}
