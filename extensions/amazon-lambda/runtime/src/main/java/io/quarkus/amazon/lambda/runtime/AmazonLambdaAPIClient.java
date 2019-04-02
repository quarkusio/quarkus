package io.quarkus.amazon.lambda.runtime;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

@Path("/2018-06-01/runtime")
public interface AmazonLambdaAPIClient {

    @Path("/invocation/next")
    @GET
    Response next();

    @Path("/invocation/{requestId}/response")
    @POST
    void respond(@PathParam("requestId") String requestId, APIGatewayProxyResponseEvent event);

    @Path("/invocation/{requestId}/error")
    @POST
    void error(@PathParam("requestId") String requestId, String body);
}
