package io.quarkus.resteasy.reactive.server.test.http2;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * A reactive route that reads the body and then hands the request over to a REST resource with
 * {@code rc.next()} must work over HTTP/2 as it does over HTTP/1.1.
 */
public class Http2RouteNextToRestTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(HelloResource.class, ForwardingRoute.class));

    @TestHTTPResource("/hello")
    URL url;

    @Test
    public void http2ClearTextUpgrade() throws Exception {
        HttpResponse<Buffer> response = post(new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(true));
        Assertions.assertEquals(HttpVersion.HTTP_2, response.version());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("hello juan", response.bodyAsString());
    }

    @Test
    public void http2ClearTextPriorKnowledge() throws Exception {
        HttpResponse<Buffer> response = post(new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false));
        Assertions.assertEquals(HttpVersion.HTTP_2, response.version());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("hello juan", response.bodyAsString());
    }

    @Test
    public void http11() throws Exception {
        HttpResponse<Buffer> response = post(new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_1_1));
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("hello juan", response.bodyAsString());
    }

    private HttpResponse<Buffer> post(WebClientOptions options) throws ExecutionException, InterruptedException {
        Vertx vertx = Vertx.vertx();
        try {
            CompletableFuture<HttpResponse<Buffer>> result = new CompletableFuture<>();
            WebClient.create(vertx, options)
                    .post(url.getPort(), url.getHost(), url.getPath())
                    .sendBuffer(Buffer.buffer("juan"))
                    .onComplete(ar -> {
                        if (ar.succeeded()) {
                            result.complete(ar.result());
                        } else {
                            result.completeExceptionally(ar.cause());
                        }
                    });
            return result.get();
        } finally {
            vertx.close();
        }
    }

    @Path("/hello")
    public static class HelloResource {

        @POST
        public String post(String body) {
            return "hello " + body;
        }
    }

    @ApplicationScoped
    public static class ForwardingRoute {

        @Route(methods = HttpMethod.POST, path = "/hello")
        void forward(RoutingContext rc) {
            rc.next();
        }
    }
}
