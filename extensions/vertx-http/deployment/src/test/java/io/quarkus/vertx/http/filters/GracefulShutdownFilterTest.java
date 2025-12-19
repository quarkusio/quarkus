package io.quarkus.vertx.http.filters;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.quarkus.runtime.shutdown.ShutdownRecorder;
import io.restassured.internal.RequestSpecificationImpl;
import io.vertx.core.http.HttpHeaders;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.web.Router;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(given.getBaseUri() + ":" + given.getPort()))
                .GET()
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertThat(response.headers().firstValue(HttpHeaders.CONNECTION.toString())).isEmpty();
        Assertions.assertThat(response.body()).isEqualTo("h2");
        // client.close();

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
