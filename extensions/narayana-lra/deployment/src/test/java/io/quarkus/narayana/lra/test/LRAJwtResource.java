package io.quarkus.narayana.lra.test;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

import io.narayana.lra.BearerTokenResolver;
import io.narayana.lra.PropagateToken;

/**
 * Test resource exercising the Narayana LRA 2.x JWT bearer-token feature.
 */
@ApplicationScoped
@Path("/lra-jwt")
public class LRAJwtResource {

    /**
     * Unauthenticated endpoint used as the boot check: a 200 here proves the
     * combined narayana-lra + smallrye-jwt deployment (including the
     * {@code @PropagateToken @LRA} endpoint below) started cleanly.
     */
    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "pong";
    }

    /**
     * Returns the raw JWT that Narayana's {@link BearerTokenResolver} obtains
     * from the CDI {@code JsonWebToken} (produced by quarkus-smallrye-jwt) for
     * the current request. This is exactly the participant-side path LRA uses
     * to capture the token before propagating it to the coordinator. Returns an
     * empty string when no token can be resolved (graceful degradation).
     */
    @GET
    @Path("/resolved")
    @Produces(MediaType.TEXT_PLAIN)
    public String resolved() {
        String token = BearerTokenResolver.resolveFromCdi();
        return token == null ? "" : token;
    }

    /**
     * Present only so the deployment must process {@code @PropagateToken} on an
     * {@code @LRA} endpoint with the MP-JWT API on the classpath. It is not
     * invoked by the tests, since starting an LRA would require a running
     * coordinator.
     */
    @GET
    @Path("/lra")
    @Produces(MediaType.TEXT_PLAIN)
    @LRA(LRA.Type.REQUIRES_NEW)
    @PropagateToken
    public String inLra() {
        return "in-lra";
    }

    /**
     * Compensation handler required for {@link #inLra()} to be a valid LRA
     * participant. Never invoked by the tests (no coordinator is contacted).
     */
    @PUT
    @Path("/compensate")
    @Produces(MediaType.TEXT_PLAIN)
    @Compensate
    public Response compensate(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(ParticipantStatus.Compensated.name()).build();
    }

    /**
     * Completion handler for the participant. Never invoked by the tests.
     */
    @PUT
    @Path("/complete")
    @Produces(MediaType.TEXT_PLAIN)
    @Complete
    public Response complete(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(ParticipantStatus.Completed.name()).build();
    }
}
