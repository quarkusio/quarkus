package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import javax.ws.rs.PUT;

public interface ParameterSubResInternalInterface<T extends Number> {
    @PUT
    void foo(T value);
}
