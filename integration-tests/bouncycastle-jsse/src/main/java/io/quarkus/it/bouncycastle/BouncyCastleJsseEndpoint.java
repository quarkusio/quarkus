package io.quarkus.it.bouncycastle;

import java.security.Security;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/jsse")
public class BouncyCastleJsseEndpoint {

    @GET
    @Path("listProviders")
    public String listProviders() {
        return Arrays.asList(Security.getProviders()).stream()
                .map(p -> p.getName()).collect(Collectors.joining(","));
    }
}
