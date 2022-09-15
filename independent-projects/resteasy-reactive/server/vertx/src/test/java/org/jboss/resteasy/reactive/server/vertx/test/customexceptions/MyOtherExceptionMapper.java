package org.jboss.resteasy.reactive.server.vertx.test.customexceptions;

import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

public class MyOtherExceptionMapper {

    private final SomeBean someBean;

    public MyOtherExceptionMapper(SomeBean someBean) {
        this.someBean = someBean;
    }

    @ServerExceptionMapper
    public Response handleMyOtherException(RoutingContext routingContext, MyOtherException myOtherException,
            SimpleResourceInfo simplifiedResourceInfo) {
        return Response.status(411).build();
    }
}
