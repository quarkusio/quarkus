package io.quarkus.it.bouncycastle;

import java.security.Security;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.security.identity.SecurityIdentity;

@Path("/jsse")
public class BouncyCastleJsseEndpoint {

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("listProviders")
    public String listProviders() {
        return "Identity: " + identity.getPrincipal().getName()
                + ", providers:" + Arrays.asList(Security.getProviders()).stream()
                        .map(p -> p.getName()).collect(Collectors.joining(","));
    }
}
