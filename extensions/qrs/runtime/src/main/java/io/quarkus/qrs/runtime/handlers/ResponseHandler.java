package io.quarkus.qrs.runtime.handlers;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.qrs.runtime.core.JSONBMessageBodyWriter;
import io.quarkus.qrs.runtime.core.RequestContext;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

public class ResponseHandler implements RestHandler {

    private final JSONBMessageBodyWriter writer = new JSONBMessageBodyWriter();

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        MediaType produces = requestContext.getTarget().getProduces();
        if (produces != null) {
            requestContext.getContext().response().headers().add(HttpHeaderNames.CONTENT_TYPE, produces.toString());
        }
        
        HttpServerResponse vertxResponse = requestContext.getContext().response();
        
        Object result = requestContext.getResult();
        Object entity = null;
        if(result instanceof Response) {
            Response response = (Response) result;
            entity = response.getEntity();
            vertxResponse.setStatusCode(response.getStatus());
            vertxResponse.setStatusMessage(response.getStatusInfo().getReasonPhrase());
            MultivaluedMap<String,String> headers = response.getStringHeaders();
            for (Entry<String, List<String>> entry : headers.entrySet()) {
                vertxResponse.putHeader(entry.getKey(), entry.getValue());
            }
        } else {
            // FIXME: custom status codes depending on method?
            vertxResponse.setStatusCode(200);
            entity = result;
        }
        
        if(entity != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writer.writeTo(entity, null, null, null, produces, null, baos);
            requestContext.getContext().response().end(Buffer.buffer(baos.toByteArray()));
        } else {
            requestContext.getContext().response().end();
        }
    }
}
