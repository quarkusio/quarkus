package org.jboss.resteasy.reactive.server.vertx.test.simple;

import java.io.Serializable;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class FeatureMappedExceptionMapper implements Cloneable, ExceptionMapper<FeatureMappedException>, Serializable {

    @Override
    public Response toResponse(FeatureMappedException exception) {
        return Response.status(667).build();
    }
}
