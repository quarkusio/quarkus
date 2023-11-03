package io.quarkus.restclient.registerprovider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.core.ResteasyContext;
import org.junit.jupiter.api.Assertions;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;

@Path("/echo")
public class EchoResource {

    @RestClient
    EchoClient client;

    @Inject
    MyRequestBean requestBean;

    @Inject
    MethodsCollector methodsCollector;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String echo(@QueryParam("message") String message) {
        return message;
    }

    @Path("call-client")
    @GET
    public String callClient() {
        // make sure we have a request context
        ManagedContext requestContext = Arc.container().requestContext();
        Assertions.assertTrue(requestContext.isActive());
        Assertions.assertNotNull(requestBean);
        // this should not end up in the client filter context
        ResteasyContext.pushContext(String.class, "callClient SERVER CONTEXT");
        // call the client
        String ret = client.calledFromClient(requestBean.getUniqueNumber());
        // make sure the filter got the same request context as we have
        Assertions.assertEquals(requestBean.getUniqueNumber(), methodsCollector.getRequestBeanFromFilter());
        // should not have passed from the client context to here
        Assertions.assertNull(ResteasyContext.getContextData(Long.class));
        return ret;
    }

    @Path("called-from-client")
    @GET
    public String calledFromClient(@QueryParam("uniqueNumber") int uniqueNumber) {
        // make sure we have a different request context to call-client
        ManagedContext requestContext = Arc.container().requestContext();
        Assertions.assertTrue(requestContext.isActive());
        Assertions.assertNotNull(requestBean);
        Assertions.assertNotEquals(uniqueNumber, requestBean.getUniqueNumber());
        // should not have passed from call-client to here
        Assertions.assertNull(ResteasyContext.getContextData(String.class));
        // should not have passed from call-client to here
        Assertions.assertNull(ResteasyContext.getContextData(Long.class));
        return "OK";
    }

    @Path("async/call-client")
    @GET
    public CompletionStage<String> asyncCallClient() {
        // make sure we have a request context
        ManagedContext requestContext = Arc.container().requestContext();
        Assertions.assertTrue(requestContext.isActive());
        Assertions.assertNotNull(requestBean);
        int req = requestBean.getUniqueNumber();
        // this should not end up in the client filter context
        ResteasyContext.pushContext(String.class, "callClient SERVER CONTEXT");
        // call the client
        return client.asyncCalledFromClient(req)
                .thenApply(ret -> {
                    // make sure the filter got the same request context as we have
                    Assertions.assertEquals(req, methodsCollector.getRequestBeanFromFilter());
                    // should not have passed from the client context to here
                    Assertions.assertNull(ResteasyContext.getContextData(Long.class));
                    return ret;
                });
    }

    @Path("async/called-from-client")
    @GET
    public CompletionStage<String> asyncCalledFromClient(@QueryParam("uniqueNumber") int uniqueNumber) {
        // make sure we have a different request context to call-client
        ManagedContext requestContext = Arc.container().requestContext();
        Assertions.assertTrue(requestContext.isActive());
        Assertions.assertNotNull(requestBean);
        Assertions.assertNotEquals(uniqueNumber, requestBean.getUniqueNumber());
        // should not have passed from call-client to here
        Assertions.assertNull(ResteasyContext.getContextData(String.class));
        // should not have passed from call-client to here
        Assertions.assertNull(ResteasyContext.getContextData(Long.class));
        return CompletableFuture.completedFuture("OK");
    }
}
