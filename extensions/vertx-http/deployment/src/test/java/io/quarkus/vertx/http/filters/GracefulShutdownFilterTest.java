package io.quarkus.vertx.http.filters;

import static io.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.quarkus.runtime.shutdown.ShutdownRecorder;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.Router;

public class GracefulShutdownFilterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, GracefulShutdownFilterTest.class))
            .overrideConfigKey("quarkus.shutdown.timeout", "10");

    @Inject
    Vertx vertx;

    @Test
    public void test() throws URISyntaxException, IOException, InterruptedException {
        get("/").then().statusCode(200)
                .header(HttpHeaders.CONNECTION.toString(), is(not((HttpHeaderValues.CLOSE.toString()))))
                .body(is("http/1.1"));

        ShutdownRecorder.runShutdown();

        testWithJdkHttpClientAndHttp2AfterShutdown();

        testWithVertxHttpClientAndHttp2AfterShutdown();

        testWithRestAssuredAndHttp11AfterShutdown();
    }

    private void testWithRestAssuredAndHttp11AfterShutdown() {
        get("/").then().statusCode(200)
                .header(HttpHeaders.CONNECTION.toString(), is(HttpHeaderValues.CLOSE.toString()))
                .body(is("http/1.1"));
    }

    private void testWithVertxHttpClientAndHttp2AfterShutdown() throws InterruptedException {
        HttpClient client = vertx.createHttpClient(new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setDefaultHost("localhost")
                .setDefaultPort(RestAssured.port));

        CountDownLatch responseLatch = new CountDownLatch(1);

        // Send a GET request
        Future<HttpClientResponse> response = client
                .request(HttpMethod.GET, "/")
                .compose(HttpClientRequest::send)
                .andThen(r -> r.result().body().onComplete(buffer -> responseLatch.countDown()));

        if (!responseLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout waiting for response");
        }

        if (response.failed()) {
            throw new RuntimeException(response.cause());
        }

        if (response.result().body().failed()) {
            throw new RuntimeException(response.result().body().cause());
        }

        Assertions.assertThat(response.result().body().result().toString()).isEqualTo("h2");
        Assertions.assertThat(response.result().headers().get(HttpHeaders.CONNECTION.toString())).isNull();
        client.close();
    }

    private void testWithJdkHttpClientAndHttp2AfterShutdown()
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(RestAssured.baseURI + ":" + RestAssured.port))
                .GET()
                .build();
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertThat(response.headers().firstValue(HttpHeaders.CONNECTION.toString())).isEmpty();
        Assertions.assertThat(response.body()).isEqualTo("h2");
    }

    @ApplicationScoped
    public static class MyBean {

        public void register(@Observes Router router) {
            router
                    .get("/")
                    .handler(rc -> rc.response().end(rc.request().version().alpnName()));
        }

    }

}
