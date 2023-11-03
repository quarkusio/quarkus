package io.quarkus.it.rest.client.main;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
