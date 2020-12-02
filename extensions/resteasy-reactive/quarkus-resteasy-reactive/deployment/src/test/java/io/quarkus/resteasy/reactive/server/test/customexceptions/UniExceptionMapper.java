package io.quarkus.resteasy.reactive.server.test.customexceptions;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.smallrye.mutiny.Uni;

public class UniExceptionMapper {

    @ServerExceptionMapper(UniException.class)
    Uni<Response> handleUni() {
        return Uni.createFrom().deferred(() -> Uni.createFrom().item(Response.status(413).build()));
    }

    @ServerExceptionMapper(ExtendsUniException.class)
    Uni<Response> handleExtendsUni() {
        return Uni.createFrom().deferred(() -> Uni.createFrom().item(Response.status(414).build()));
    }
}
