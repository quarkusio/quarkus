package io.quarkus.it.openapi.jaxrs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;

@Path("/jax-rs/defaultContentType")
public class URLResource {

    @GET
    @Path("/justURL")
    public URL justURL() {
        return url();
    }

    @POST
    @Path("/justURL")
    public URL justURL(URL url) {
        return url;
    }

    @GET
    @Path("/restResponseURL")
    public RestResponse<URL> restResponseURL() {
        return RestResponse.ok(url());
    }

    @POST
    @Path("/restResponseURL")
    public RestResponse<URL> restResponseURL(URL body) {
        return RestResponse.ok(body);
    }

    @GET
    @Path("/optionalURL")
    public Optional<URL> optionalURL() {
        return Optional.of(url());
    }

    @POST
    @Path("/optionalURL")
    public Optional<URL> optionalURL(Optional<URL> body) {
        return body;
    }

    @GET
    @Path("/uniURL")
    public Uni<URL> uniURL() {
        return Uni.createFrom().item(url());
    }

    @GET
    @Path("/completionStageURL")
    public CompletionStage<URL> completionStageURL() {
        return CompletableFuture.completedStage(url());
    }

    @GET
    @Path("/completedFutureURL")
    public CompletableFuture<URL> completedFutureURL() {
        return CompletableFuture.completedFuture(url());
    }

    @GET
    @Path("/listURL")
    public List<URL> listURL() {
        return Arrays.asList(new URL[] { url() });
    }

    @POST
    @Path("/listURL")
    public List<URL> listURL(List<URL> body) {
        return body;
    }

    @GET
    @Path("/arrayURL")
    public URL[] arrayURL() {
        return new URL[] { url() };
    }

    @POST
    @Path("/arrayURL")
    public URL[] arrayURL(URL[] body) {
        return body;
    }

    @GET
    @Path("/mapURL")
    public Map<String, URL> mapURL() {
        Map<String, URL> m = new HashMap<>();
        m.put("mapURL", url());
        return m;
    }

    @POST
    @Path("/mapURL")
    public Map<String, URL> mapURL(Map<String, URL> body) {
        return body;
    }

    private URL url() {
        try {
            return new URL("https://quarkus.io/");
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
