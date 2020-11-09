package io.quarkus.rest.server.test.resteasy.async.filters;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.core.QuarkusRestContext;

@Path("/")
public class AsyncRequestFilterResource {

    private static final Logger LOG = Logger.getLogger(AsyncRequestFilterResource.class);

    @GET
    public Response threeSyncRequestFilters(@Context QuarkusRestContext ctx,
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
        //            return Response.serverError().entity("Request suspention is wrong").build();
        return Response.ok("resource").build();
    }

    @Path("non-response")
    @GET
    public String threeSyncRequestFiltersNonResponse(@Context QuarkusRestContext ctx,
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
        //            throw new WebApplicationException(Response.serverError().entity("Request suspention is wrong").build());
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
