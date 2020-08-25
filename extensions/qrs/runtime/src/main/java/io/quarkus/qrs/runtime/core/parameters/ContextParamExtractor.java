package io.quarkus.qrs.runtime.core.parameters;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.sse.SseEventSink;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.jaxrs.QrsAsyncResponse;
import io.quarkus.qrs.runtime.jaxrs.QrsSseEventSink;

public class ContextParamExtractor implements ParameterExtractor {

    private String type;

    public ContextParamExtractor(String type) {
        this.type = type;
    }

    @Override
    public Object extractParameter(QrsRequestContext context) {
        if (type.equals(HttpHeaders.class.getName())) {
            return context.getHttpHeaders();
        }
        if (type.equals(QrsRequestContext.class.getName())) {
            return context;
        }
        if (type.equals(UriInfo.class.getName())) {
            return context.getUriInfo();
        }
        if (type.equals(AsyncResponse.class.getName())) {
            QrsAsyncResponse response = new QrsAsyncResponse(context);
            context.setAsyncResponse(response);
            return response;
        }
        if (type.equals(SseEventSink.class.getName())) {
            QrsSseEventSink sink = new QrsSseEventSink(context);
            context.setSseEventSink(sink);
            return sink;
        }
        // FIXME: move to build time
        throw new IllegalStateException("Unsupported contextual type: " + type);
    }

}
