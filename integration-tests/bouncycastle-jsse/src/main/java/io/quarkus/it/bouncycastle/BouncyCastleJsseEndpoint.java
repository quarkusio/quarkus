package io.quarkus.it.bouncycastle;

import java.security.Security;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
