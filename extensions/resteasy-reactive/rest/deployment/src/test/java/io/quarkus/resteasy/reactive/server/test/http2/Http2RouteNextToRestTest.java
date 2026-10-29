package io.quarkus.resteasy.reactive.server.test.http2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.quarkus.vertx.web.RouteFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

/**
 * A reactive route that reads the body and then hands the request over to a REST resource with
 * {@code rc.next()} must work over HTTP/2 as it does over HTTP/1.1.
 */
public class Http2RouteNextToRestTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(HelloResource.class, ForwardingRoute.class,
                    StreamResource.class, FormResource.class, BodyReadingFilter.class));

    @Inject
    Vertx vertx;

    @TestHTTPResource("/hello")
    URL url;

    @TestHTTPResource("/stream")
    URL streamUrl;

    @TestHTTPResource("/form")
    URL formUrl;

    @Test
    public void http2ClearTextUpgrade() {
        HttpResponse<Buffer> response = post(url, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(true), Buffer.buffer("juan"), null);
        Assertions.assertEquals(HttpVersion.HTTP_2, response.version());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("hello juan", response.bodyAsString());
    }

    @Test
    public void http2ClearTextPriorKnowledge() {
        HttpResponse<Buffer> response = post(url, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false), Buffer.buffer("juan"), null);
        Assertions.assertEquals(HttpVersion.HTTP_2, response.version());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("hello juan", response.bodyAsString());
    }

    @Test
    public void http11() {
        HttpResponse<Buffer> response = post(url, new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_1_1),
                Buffer.buffer("juan"), null);
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("hello juan", response.bodyAsString());
    }

    @Test
    public void http2StreamWithoutBody() {
        HttpResponse<Buffer> response = post(streamUrl, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false), Buffer.buffer(), null);
        Assertions.assertEquals(HttpVersion.HTTP_2, response.version());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("read 0", response.bodyAsString());
    }

    @Test
    public void http11StreamWithoutBody() {
        HttpResponse<Buffer> response = post(streamUrl, new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_1_1),
                Buffer.buffer(), null);
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("read 0", response.bodyAsString());
    }

    @Test
    public void http2FormAfterFilterReadTheBody() {
        HttpResponse<Buffer> response = post(formUrl, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false), Buffer.buffer("name=juan"), MediaType.APPLICATION_FORM_URLENCODED);
        Assertions.assertEquals(HttpVersion.HTTP_2, response.version());
        Assertions.assertEquals(500, response.statusCode());
    }

    @Test
    public void http11FormAfterFilterReadTheBody() {
        HttpResponse<Buffer> response = post(formUrl, new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_1_1),
                Buffer.buffer("name=juan"), MediaType.APPLICATION_FORM_URLENCODED);
        Assertions.assertEquals(500, response.statusCode());
    }

    private HttpResponse<Buffer> post(URL target, WebClientOptions options, Buffer body, String contentType) {
        WebClient client = WebClient.create(vertx, options);
        try {
            HttpRequest<Buffer> request = client.post(target.getPort(), target.getHost(), target.getPath());
            if (contentType != null) {
                request.putHeader("content-type", contentType);
            }
            return request.sendBuffer(body).await().atMost(REQUEST_TIMEOUT);
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

    @Path("/form")
    public static class FormResource {

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public Uni<String> post(@FormParam("name") String name) {
            return Uni.createFrom().item("name=" + name);
        }
    }

    /**
     * Reads the body of {@code /form} itself, so the request has ended and nothing is buffered in the routing
     * context when the resource parses the form. The resource cannot read the form, so the request fails.
     */
    @ApplicationScoped
    public static class BodyReadingFilter {

        @RouteFilter
        void readBody(RoutingContext rc) {
            if (!"/form".equals(rc.normalizedPath())) {
                rc.next();
                return;
            }
            rc.request().handler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer buffer) {
                }
            }).endHandler(new Handler<Void>() {
                @Override
                public void handle(Void unused) {
                    rc.next();
                }
            });
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
