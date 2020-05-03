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

@Path("txns")
public class TxnResource {
    static AtomicInteger completions = new AtomicInteger(0);
    static AtomicInteger compensations = new AtomicInteger(0);

    @GET
    @Path("completions")
    public int completions() {
        return completions.get();
    }

    @GET
    @Path("compensations")
    public int compensations() {
        return compensations.get();
    }

    @POST
    @Path("tx")
    @LRA
    public String doInTx(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return lraId.toASCIIString();
    }

    @PUT
    @Path("compensate")
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId, String userData) {
        if (lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        compensations.incrementAndGet();

        return Response.ok(lraId.toASCIIString()).build();
    }

    @PUT
    @Path("complete")
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId, String userData) {
        if (lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        completions.incrementAndGet();

        return Response.ok(lraId.toASCIIString()).build();
    }
}
