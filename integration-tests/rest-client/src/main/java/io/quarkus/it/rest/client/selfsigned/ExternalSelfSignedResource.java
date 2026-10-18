package io.quarkus.it.rest.client.selfsigned;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

/**
 * This has nothing to do with rest-client, but we add it here in order to avoid creating
 * a new integration test that would slow down our CI
 */
@Path("/self-signed")
public class ExternalSelfSignedResource {

    @RestClient
    ExternalSelfSignedClient externalSelfSignedClient;

    @Inject
    TlsConfigurationRegistry tlsConfigurationRegistry;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "self-signed.port", defaultValue = "-1")
    int serverPort;

    @GET
    @Path("/ExternalSelfSignedClient")
    @Produces(MediaType.TEXT_PLAIN)
    public Response perform() {
        return externalSelfSignedClient.invoke();
    }

    @GET
    @Path("/HttpClient/{tlsConfigName}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response client(@PathParam("tlsConfigName") String tlsConfigName)
            throws InterruptedException, ExecutionException, TimeoutException {
        final HttpClientOptions opts = new HttpClientOptions();
        tlsConfigurationRegistry.get(tlsConfigName)
                .ifPresent(tlsConfig -> opts.setTrustOptions(tlsConfig.getTrustStoreOptions()));
        final HttpClient client = vertx.createHttpClient(opts);
        final Future<Response> response = client.request(
                new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setHost("localhost")
                        .setPort(serverPort)
                        .setURI("/")
                        .setSsl(true))
                .compose(request -> request.end().compose(x -> request.response())
                        .compose(resp -> resp
                                .body()
                                .map(Buffer::toString)
                                .compose(respBody -> Future.succeededFuture(Response.ok(respBody).build()))))
                .recover(e -> Future.succeededFuture(Response.status(500).entity(stackTrace(e)).build()));
        try {
            return response.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        } finally {
            client.close();
        }
    }

    static Object stackTrace(Throwable e) {
        final Writer sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
