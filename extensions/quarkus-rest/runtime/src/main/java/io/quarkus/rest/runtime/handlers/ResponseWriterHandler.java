package io.quarkus.rest.runtime.handlers;

import javax.ws.rs.core.Response;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.core.serialization.DynamicEntityWriter;
import io.quarkus.rest.runtime.core.serialization.EntityWriter;

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
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        Response response = requestContext.getResponse();
        // has been converted in ResponseHandler
        //TODO: should we do this the other way around so there is no need to allocate the Response object

        Object entity = response.getEntity();
        if (entity != null && !requestContext.getMethod().equals(HEAD)) {
            EntityWriter entityWriter = requestContext.getEntityWriter();
            if (entityWriter == null) {
                dynamicEntityWriter.write(requestContext, entity);
            } else {
                entityWriter.write(requestContext, entity);
            }
        } else {
            Serialisers.encodeResponseHeaders(requestContext);
            requestContext.getContext().response().end();
        }
    }
}
