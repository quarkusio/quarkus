package io.quarkus.resteasy.reactive.server.test.resteasy.async.filters;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Path("/")
public class AsyncRequestFilterResource {

    private static final Logger LOG = Logger.getLogger(AsyncRequestFilterResource.class);

    @GET
    public Response threeSyncRequestFilters(@Context ServerRequestContext ctx,
            @HeaderParam("Filter1") @DefaultValue("") String filter1,
            @HeaderParam("Filter2") @DefaultValue("") String filter2,
            @HeaderParam("Filter3") @DefaultValue("") String filter3,
            @HeaderParam("PreMatchFilter1") @DefaultValue("") String preMatchFilter1,
            @HeaderParam("PreMatchFilter2") @DefaultValue("") String preMatchFilter2,
            @HeaderParam("PreMatchFilter3") @DefaultValue("") String preMatchFilter3) {
        //        boolean async = isAsync(filter1)
        //                || isAsync(filter2)
        //                || isAsync(filter3)
        //                || isAsync(preMatchFilter1)
        //                || isAsync(preMatchFilter2)
        //                || isAsync(preMatchFilter3);
        //        if (async != ctx.isSuspended())
        //            return Response.serverError().entity("Request suspension is wrong").build();
        return Response.ok("resource").build();
    }

    @Path("non-response")
    @GET
    public String threeSyncRequestFiltersNonResponse(@Context ServerRequestContext ctx,
            @HeaderParam("Filter1") @DefaultValue("") String filter1,
            @HeaderParam("Filter2") @DefaultValue("") String filter2,
            @HeaderParam("Filter3") @DefaultValue("") String filter3,
            @HeaderParam("PreMatchFilter1") @DefaultValue("") String preMatchFilter1,
            @HeaderParam("PreMatchFilter2") @DefaultValue("") String preMatchFilter2,
            @HeaderParam("PreMatchFilter3") @DefaultValue("") String preMatchFilter3) {
        //        boolean async = isAsync(filter1)
        //                || isAsync(filter2)
        //                || isAsync(filter3)
        //                || isAsync(preMatchFilter1)
        //                || isAsync(preMatchFilter2)
        //                || isAsync(preMatchFilter3);
        //        if (async != ctx.isSuspended())
        //            throw new WebApplicationException(Response.serverError().entity("Request suspension is wrong").build());
        return "resource";
    }

    @Path("async")
    @GET
    public CompletionStage<Response> async() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<Response> resp = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                LOG.error("Error:", e);
            }
            resp.complete(Response.ok("resource").build());
        });
        return resp;
    }

    @Path("callback")
    @GET
    public String callback() {
        return "hello";
    }

    @Path("callback-async")
    @GET
    public CompletionStage<String> callbackAsync() {
        return CompletableFuture.completedFuture("hello");
    }

    private boolean isAsync(String filter) {
        return filter.equals("async-pass")
                || filter.equals("async-fail");
    }
}
