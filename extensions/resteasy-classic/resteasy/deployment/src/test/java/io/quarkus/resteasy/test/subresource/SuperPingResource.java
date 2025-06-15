package io.quarkus.resteasy.test.subresource;

import jakarta.ws.rs.GET;

public class SuperPingResource extends PingResource {

    @Override
    @GET
    public String ping() {
        return "super" + service.ping();
    }

}
