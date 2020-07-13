package io.quarkus.qrs.runtime.handlers;

import java.io.ByteArrayOutputStream;

import javax.ws.rs.core.MediaType;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.qrs.runtime.core.JSONBMessageBodyWriter;
import io.quarkus.qrs.runtime.core.RequestContext;
import io.vertx.core.buffer.Buffer;

public class ResponseHandler implements RestHandler {

    private final JSONBMessageBodyWriter writer = new JSONBMessageBodyWriter();

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        MediaType produces = requestContext.getTarget().getProduces();
        if (produces != null) {
            requestContext.getContext().response().headers().add(HttpHeaderNames.CONTENT_TYPE, produces.toString());
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.writeTo(requestContext.getResult(), null, null, null, produces, null, baos);
        requestContext.getContext().response().end(Buffer.buffer(baos.toByteArray()));
    }
}
