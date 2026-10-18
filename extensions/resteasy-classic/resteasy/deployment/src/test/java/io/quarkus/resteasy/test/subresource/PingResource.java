package io.quarkus.resteasy.test.subresource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;

public class PingResource {

    @Inject
    MyService service;

    @GET
    public String ping() {
        return service.ping();
    }

}
