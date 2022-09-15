package org.jboss.resteasy.reactive.server.vertx.test.customexceptions;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class UniExceptionMapper {

    @ServerExceptionMapper({ UniException.class, OtherUniException.class })
    Uni<Response> handleUni(Throwable t) {
        return Uni.createFrom().deferred(() -> Uni.createFrom().item(Response.status(413).build()));
    }

    @ServerExceptionMapper(ExtendsUniException.class)
    Uni<Response> handleExtendsUni() {
        return Uni.createFrom().deferred(() -> Uni.createFrom().item(Response.status(414).build()));
    }
}
