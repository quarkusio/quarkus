package io.quarkus.qrs.runtime.handlers;

import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.serialization.DynamicEntityWriter;
import io.quarkus.qrs.runtime.core.serialization.EntityWriter;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;

/**
 * Our job is to write a Response
 */
public class ResponseWriterHandler implements RestHandler {

    public static final String HEAD = "HEAD";
    private final DynamicEntityWriter dynamicEntityWriter;

    public ResponseWriterHandler(DynamicEntityWriter dynamicEntityWriter) {
        this.dynamicEntityWriter = dynamicEntityWriter;
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        HttpServerResponse vertxResponse = requestContext.getContext().response();

        Response response = requestContext.getResponse();
        // has been converted in ResponseHandler
        //TODO: should we do this the other way around so there is no need to allocate the Response object
        requestContext.getContext().addHeadersEndHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                vertxResponse.setStatusCode(response.getStatus());
                if (response.getStatusInfo().getReasonPhrase() != null)
                    vertxResponse.setStatusMessage(response.getStatusInfo().getReasonPhrase());
                MultivaluedMap<String, String> headers = response.getStringHeaders();
                for (Entry<String, List<String>> entry : headers.entrySet()) {
                    vertxResponse.putHeader(entry.getKey(), entry.getValue());
                }
            }
        });

        Object entity = response.getEntity();
        if (entity != null && !requestContext.getMethod().equals(HEAD)) {
            EntityWriter entityWriter = requestContext.getEntityWriter();
            if (entityWriter == null) {
                dynamicEntityWriter.write(requestContext, entity);
            } else {
                entityWriter.write(requestContext, entity);
            }
        } else {
            requestContext.getContext().response().end();
        }
    }
}
