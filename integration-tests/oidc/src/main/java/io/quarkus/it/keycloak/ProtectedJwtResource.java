package io.quarkus.it.keycloak;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.SecurityIdentityAssociation;

@Path("/web-app")
@Authenticated
public class ProtectedJwtResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken accessToken;

    @Context
    SecurityContext securityContext;

    @Inject
    SecurityIdentityAssociation identityAssociation;
    @Inject
    SecurityContextExecutor securityContextExecutor;

    ManagedExecutor executor = ManagedExecutor.builder().cleared(ThreadContext.SECURITY).build();

    @GET
    @Path("test-security")
    public String testSecurity() {
        return securityContext.getUserPrincipal().getName();
    }

    @GET
    @Path("test-security-jwt")
    public String testSecurityJwt() {
        return accessToken.getName() + ":" + accessToken.getGroups().iterator().next()
                + ":" + accessToken.getClaim("email");
    }

    @GET
    @Path("test-security-propagation")
    public String testSecurityPropagation() {
        CompletableFuture<SecurityIdentity> future = executor.supplyAsync(
                () -> securityContextExecutor.executeUsingIdentity(accessToken.getRawToken(),
                        identityAssociation::getIdentity));
        try {
            return future.get().getPrincipal().getName();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
