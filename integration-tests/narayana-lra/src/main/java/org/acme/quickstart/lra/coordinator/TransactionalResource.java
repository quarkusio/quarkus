package org.acme.quickstart.lra.coordinator;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

@Path("/tx")
public class TransactionalResource {
    static AtomicInteger completions = new AtomicInteger(0);
    static AtomicInteger compensations = new AtomicInteger(0);

    // run a method that starts an LRA on entry and does not end it on exit
    @POST
    @Path("/start")
    @LRA(end = false)
    public Response startTx(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.created(lraId).entity(lraId).build();
    }

    // run a method must be called with an active LRA and ends the LRA on exit
    @PUT
    @Path("/end")
    @LRA(LRA.Type.MANDATORY)
    public String endTx(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return lraId.toASCIIString();
    }

    // run a method that starts an LRA on entry and ends it on exit
    @POST
    @Path("/lra")
    @LRA
    public Response doInTx(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.created(lraId).entity(lraId).build();
    }

    // return the number of times the completion callback was called
    @GET
    @Path("completions")
    public int completions() {
        return completions.get();
    }

    // return the number of times the compensation callback was called
    @GET
    @Path("compensations")
    public int compensations() {
        return compensations.get();
    }

    // callback to inform the participant service that the LRA is cancelling
    @PUT
    @Path("compensate")
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        if (lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        compensations.incrementAndGet();

        return Response.ok(lraId.toASCIIString()).build();
    }

    // callback to inform the participant service that the LRA is closing
    @PUT
    @Path("complete")
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        if (lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        completions.incrementAndGet();

        return Response.ok(lraId.toASCIIString()).build();
    }
}
