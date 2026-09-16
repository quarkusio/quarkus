package io.quarkus.resteasy.reactive.server.test.http2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
            .withApplicationRoot((jar) -> jar.addClasses(HelloResource.class, ForwardingRoute.class,
                    StreamResource.class));

    @Inject
    Vertx vertx;

    @TestHTTPResource("/hello")
    URL url;

    @TestHTTPResource("/stream")
    URL streamUrl;

    @Test
    public void http2ClearTextUpgrade() throws Exception {
        HttpResponse<Buffer> response = post(url, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(true));
        Assertions.assertEquals(HttpVersion.HTTP_2, response.version());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("hello juan", response.bodyAsString());
    }

    @Test
    public void http2ClearTextPriorKnowledge() throws Exception {
        HttpResponse<Buffer> response = post(url, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false));
        Assertions.assertEquals(HttpVersion.HTTP_2, response.version());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("hello juan", response.bodyAsString());
    }

    @Test
    public void http11() throws Exception {
        HttpResponse<Buffer> response = post(url, new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_1_1));
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("hello juan", response.bodyAsString());
    }

    @Test
    public void http2StreamWithoutBody() throws Exception {
        HttpResponse<Buffer> response = post(streamUrl, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false), Buffer.buffer());
        Assertions.assertEquals(HttpVersion.HTTP_2, response.version());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("read 0", response.bodyAsString());
    }

    @Test
    public void http11StreamWithoutBody() throws Exception {
        HttpResponse<Buffer> response = post(streamUrl,
                new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_1_1), Buffer.buffer());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("read 0", response.bodyAsString());
    }

    private HttpResponse<Buffer> post(URL target, WebClientOptions options)
            throws ExecutionException, InterruptedException {
        return post(target, options, Buffer.buffer("juan"));
    }

    private HttpResponse<Buffer> post(URL target, WebClientOptions options, Buffer body)
            throws ExecutionException, InterruptedException {
        CompletableFuture<HttpResponse<Buffer>> result = new CompletableFuture<>();
        WebClient client = WebClient.create(vertx, options);
        try {
            client.post(target.getPort(), target.getHost(), target.getPath())
                    .sendBuffer(body)
                    .onComplete(ar -> {
                        if (ar.succeeded()) {
                            result.complete(ar.result());
                        } else {
                            result.completeExceptionally(ar.cause());
                        }
                    });
            return result.get();
        } finally {
            client.close();
        }
    }

    @Path("/hello")
    public static class HelloResource {

        @POST
        public String post(String body) {
            return "hello " + body;
        }
    }

    @Path("/stream")
    public static class StreamResource {

        @POST
        public String post(InputStream body) throws IOException {
            return "read " + new String(body.readAllBytes(), StandardCharsets.UTF_8).length();
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
