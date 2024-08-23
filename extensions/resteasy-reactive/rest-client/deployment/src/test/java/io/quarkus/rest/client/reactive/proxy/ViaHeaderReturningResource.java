package io.quarkus.rest.client.reactive.proxy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

@Path("/resource")
public class ViaHeaderReturningResource {
    @GET
    public String getViaHeader(@HeaderParam("X-Via") String viaHeader) {
        return viaHeader == null ? "noProxy" : viaHeader;
    }
}
