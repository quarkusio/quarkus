package io.quarkus.it.rest.client.main;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;

@Path("")
@RegisterProvider(DefaultCtorTestFilter.class)
@RegisterProvider(NonDefaultCtorTestFilter.class)
public interface AppleClient {
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Apple swapApple(Apple original);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    Apple someApple();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    String stringApple();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CompletionStage<Apple> completionSwapApple(Apple original);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Apple> completionSomeApple();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<String> completionStringApple();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Apple> uniSwapApple(Apple original);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Apple> uniSomeApple();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    Uni<String> uniStringApple();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    RestResponse<Apple> restResponseApple();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    Uni<RestResponse<Apple>> uniRestResponseApple();
}
