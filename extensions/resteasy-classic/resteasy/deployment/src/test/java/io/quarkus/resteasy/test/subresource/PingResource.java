package io.quarkus.resteasy.test.subresource;

import javax.inject.Inject;
import javax.ws.rs.GET;

public class PingResource {

    @Inject
    MyService service;

    @GET
    public String ping() {
        return service.ping();
    }

}
