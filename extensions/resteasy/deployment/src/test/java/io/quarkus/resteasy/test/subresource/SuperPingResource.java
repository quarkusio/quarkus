package io.quarkus.resteasy.test.subresource;

import javax.ws.rs.GET;

public class SuperPingResource extends PingResource {

    @GET
    public String ping() {
        return "super" + service.ping();
    }

}
