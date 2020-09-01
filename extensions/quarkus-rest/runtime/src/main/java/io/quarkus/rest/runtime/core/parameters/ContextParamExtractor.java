package io.quarkus.rest.runtime.core.parameters;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.sse.SseEventSink;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestAsyncResponse;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestSseEventSink;

public class ContextParamExtractor implements ParameterExtractor {

    private String type;

    public ContextParamExtractor(String type) {
        this.type = type;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        if (type.equals(HttpHeaders.class.getName())) {
            return context.getHttpHeaders();
        }
        if (type.equals(QuarkusRestRequestContext.class.getName())) {
            return context;
        }
        if (type.equals(UriInfo.class.getName())) {
            return context.getUriInfo();
        }
        if (type.equals(AsyncResponse.class.getName())) {
            QuarkusRestAsyncResponse response = new QuarkusRestAsyncResponse(context);
            context.setAsyncResponse(response);
            return response;
        }
        if (type.equals(SseEventSink.class.getName())) {
            QuarkusRestSseEventSink sink = new QuarkusRestSseEventSink(context);
            context.setSseEventSink(sink);
            return sink;
        }
        // FIXME: move to build time
        throw new IllegalStateException("Unsupported contextual type: " + type);
    }

}
