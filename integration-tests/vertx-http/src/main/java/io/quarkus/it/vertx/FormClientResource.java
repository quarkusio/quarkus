package io.quarkus.it.vertx;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import io.vertx.core.Vertx;
import io.vertx.core.http.ClientForm;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

/**
 * Sends a form with the Vert.x HTTP client, which goes through
 * {@code io.vertx.core.http.impl.ClientMultipartFormUpload} and must be registered for runtime
 * initialization in native mode.
 */
@Path("/form-client")
public class FormClientResource {

    @Inject
    Vertx vertx;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> sendForm(@Context UriInfo uriInfo) {
        URI baseUri = uriInfo.getBaseUri();
        return vertx.createHttpClient()
                .request(HttpMethod.POST, baseUri.getPort(), baseUri.getHost(), "/form-client/echo")
                .compose(request -> request.send(ClientForm.form().attribute("name", "Quarkus")))
                .compose(HttpClientResponse::body)
                .map(Object::toString)
                .toCompletionStage();
    }

    @POST
    @Path("/echo")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String echo(@FormParam("name") String name) {
        return "Hello " + name;
    }
}
