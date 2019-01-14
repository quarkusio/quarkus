package org.jboss.panache;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class Controller {

    protected <T> T throwNotFoundIfNull(T value, String message) {
        if(value == null)
            throw new WebApplicationException(message, Status.NOT_FOUND);
        return value;
    }

    protected Response created(Object value) {
        return Response.ok(value).status(Status.CREATED).build();
    }
    
    protected Response noContent() {
        return Response.status(Status.NO_CONTENT).build();
    }
}
