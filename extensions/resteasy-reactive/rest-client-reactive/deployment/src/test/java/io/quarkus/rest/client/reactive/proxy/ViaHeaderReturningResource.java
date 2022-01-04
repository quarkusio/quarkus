package io.quarkus.rest.client.reactive.proxy;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;

@Path("/resource")
public class ViaHeaderReturningResource {
    @GET
    public String getViaHeader(@HeaderParam("X-Via") String viaHeader) {
        return viaHeader == null ? "noProxy" : viaHeader;
    }
}
