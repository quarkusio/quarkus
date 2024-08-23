package org.jboss.resteasy.reactive.server.vertx.test.response;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import io.smallrye.mutiny.Uni;

@Path("")
public class RestResponseResource {
    @GET
    @Path("rest-response")
    public RestResponse<String> getString() {
        return RestResponse.ok("Hello");
    }

    @GET
    @Path("rest-response-empty")
    public RestResponse<String> empty() {
        return RestResponse.status(499);
    }

    @GET
    @Path("response-empty")
    public Response emptyResponse() {
        return Response.status(499).build();
    }

    @GET
    @Path("rest-response-wildcard")
    public RestResponse<?> wildcard() {
        return RestResponse.ResponseBuilder.ok("Hello").header("content-type", "text/plain").build();
    }

    @GET
    @Path("rest-response-location")
    public RestResponse<?> location() {
        final var location = UriBuilder.fromResource(RestResponseResource.class).path("{language}")
                .queryParam("user", "John")
                .build("en/us");
        return RestResponse.ResponseBuilder.ok("Hello").location(location).build();
    }

    @GET
    @Path("rest-response-content-location")
    public RestResponse<?> contentLocation() {
        final var location = UriBuilder.fromResource(RestResponseResource.class).path("{language}")
                .queryParam("user", "John")
                .build("en/us");
        return RestResponse.ResponseBuilder.ok("Hello").contentLocation(location).build();
    }

    @GET
    @Path("rest-response-full")
    @SuppressWarnings("deprecation")
    public RestResponse<String> getResponse() throws URISyntaxException {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(42);
        cc.setPrivate(true);
        return RestResponse.ResponseBuilder.ok("Hello")
                .allow("FOO", "BAR")
                .cacheControl(cc)
                .contentLocation(new URI("http://example.com/content"))
                .cookie(new NewCookie("Flavour", "Praliné"))
                .encoding("Stef-Encoding")
                .expires(Date.from(Instant.parse("2021-01-01T00:00:00Z")))
                .header("X-Stef", "FroMage")
                .language(Locale.FRENCH)
                .lastModified(Date.from(Instant.parse("2021-01-02T00:00:00Z")))
                .link("http://example.com/link", "stef")
                .location(new URI("http://example.com/location"))
                .tag("yourit")
                .type("text/stef")
                .variants(Variant.languages(Locale.ENGLISH, Locale.GERMAN).build())
                .build();
    }

    @GET
    @Path("response-uni")
    public Response getResponseUniString() {
        return Response.ok(Uni.createFrom().item("Hello")).build();
    }

    @GET
    @Path("rest-response-uni")
    public RestResponse<Uni<String>> getUniString() {
        return RestResponse.ok(Uni.createFrom().item("Hello"));
    }

    @GET
    @Path("rest-response-exception")
    public String getRestResponseException() {
        throw new UnknownCheeseException1("Cheddar");
    }

    @GET
    @Path("uni-rest-response-exception")
    public String getUniRestResponseException() {
        throw new UnknownCheeseException2("Cheddar");
    }

    @ServerExceptionMapper
    public RestResponse<String> mapException(UnknownCheeseException1 x) {
        return RestResponse.status(Response.Status.NOT_FOUND, "Unknown cheese: " + x.name);
    }

    @ServerExceptionMapper
    public Uni<RestResponse<String>> mapExceptionAsync(UnknownCheeseException2 x) {
        return Uni.createFrom().item(RestResponse.status(Response.Status.NOT_FOUND, "Unknown cheese: " + x.name));
    }

    @ServerRequestFilter(preMatching = true)
    public RestResponse<String> restResponseRequestFilter(ContainerRequestContext ctx) {
        if (ctx.getUriInfo().getPath().equals("/rest-response-request-filter")) {
            return RestResponse.ok("RestResponse request filter");
        }
        return null;
    }

    @ServerRequestFilter(preMatching = true)
    public Optional<RestResponse<String>> optionalRestResponseRequestFilter(ContainerRequestContext ctx) {
        if (ctx.getUriInfo().getPath().equals("/optional-rest-response-request-filter")) {
            return Optional.of(RestResponse.ok("Optional<RestResponse> request filter"));
        }
        return Optional.empty();
    }

    @ServerRequestFilter(preMatching = true)
    public Uni<RestResponse<String>> uniRestResponseRequestFilter(ContainerRequestContext ctx) {
        if (ctx.getUriInfo().getPath().equals("/uni-rest-response-request-filter")) {
            return Uni.createFrom().item(RestResponse.ok("Uni<RestResponse> request filter"));
        }
        return null;
    }
}
