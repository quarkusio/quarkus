package io.quarkus.vertx.http.filters;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.quarkus.runtime.shutdown.ShutdownRecorder;
import io.restassured.internal.RequestSpecificationImpl;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.web.Router;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class GracefulShutdownFilterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, GracefulShutdownFilterTest.class))
            .overrideConfigKey("quarkus.shutdown.timeout", "10");

    @Test
    public void test() throws URISyntaxException, IOException, InterruptedException {
        get("/").then().statusCode(200)
                .header(HttpHeaders.CONNECTION.toString(), is(not((HttpHeaderValues.CLOSE.toString()))))
                .body(is("http/1.1"));
        RequestSpecificationImpl given = (RequestSpecificationImpl) given();

        ShutdownRecorder.runShutdown();

        /*
        // JDK 21 code unavailable on JDK 17 due to missing HTTP/2 functionality.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(given.getBaseUri() + ":" + given.getPort()))
                .GET()
                .build();
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertThat(response.headers().firstValue(HttpHeaders.CONNECTION.toString())).isEmpty();
        Assertions.assertThat(response.body()).isEqualTo("h2");
        // client.close();
        */

        Vertx vertx = Vertx.vertx();
        try {
            HttpClient client = vertx.createHttpClient(new HttpClientOptions()
                    .setProtocolVersion(HttpVersion.HTTP_2)
                    .setDefaultHost("localhost")
                    .setDefaultPort(given.getPort()));

            // Send a GET request
            HttpClientResponse response = Uni.createFrom().completionStage(client
                    .request(HttpMethod.GET, "/")
                    .compose(HttpClientRequest::send)
                    .toCompletionStage()).await().atMost(Duration.of(10, ChronoUnit.MINUTES));
            Assertions.assertThat(
                    Uni.createFrom().completionStage(response.body().toCompletionStage()).await().atMost(Duration.of(10, ChronoUnit.SECONDS)).toString()).isEqualTo("h2");
            Assertions.assertThat(response.headers().get(HttpHeaders.CONNECTION.toString())).isNull();
            client.close();
        } finally {
            vertx.close().result();
        }

        get("/").then().statusCode(200)
                .header(HttpHeaders.CONNECTION.toString(), is(HttpHeaderValues.CLOSE.toString()))
                .body(is("http/1.1"));
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
