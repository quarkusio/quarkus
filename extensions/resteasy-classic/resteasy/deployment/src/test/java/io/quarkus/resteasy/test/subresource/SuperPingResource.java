package io.quarkus.resteasy.test.subresource;

import jakarta.ws.rs.GET;

public class SuperPingResource extends PingResource {

    @GET
    public String ping() {
        return "super" + service.ping();
    }

}
