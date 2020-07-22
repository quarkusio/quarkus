package io.quarkus.qrs.runtime.handlers;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.qrs.runtime.core.RequestContext;

/**
 * Our job is to turn endpoint return types into Response instances
 */
public class ResponseHandler implements RestHandler {

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        Object result = requestContext.getResult();
        Response response = null;
        if (result instanceof Response) {
            response = (Response) result;
        } else {
            // FIXME: custom status codes depending on method?
            response = Response.ok().entity(result).build();
            requestContext.setResult(response);
        }

        MediaType produces = requestContext.getTarget().getProduces();
        if (produces != null) {
            response.getHeaders().add(HttpHeaders.CONTENT_TYPE, produces.toString());
        }
    }
}
