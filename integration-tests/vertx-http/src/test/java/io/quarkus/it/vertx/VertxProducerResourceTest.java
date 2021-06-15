package io.quarkus.it.vertx;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class VertxProducerResourceTest {

    @TestHTTPResource(ssl = true)
    URL url;

    @Test
    public void testInjection() {
        get("/").then().body(containsString("vert.x has been injected"));
    }

    @Test
    public void testInjectedRouter() {
        given().contentType("text/plain").body("Hello world!")
                .post("/").then().body(is("Hello world!"));
    }

    @Test
    public void testRouteRegistration() {
        get("/my-path").then().body(containsString("OK"));
    }

    @Test
    public void testRouteRegistrationMTLS() {
        RequestSpecification spec = new RequestSpecBuilder()
                .setBaseUri(String.format("%s://%s", url.getProtocol(), url.getHost()))
                .setPort(url.getPort())
                .setKeyStore("client-keystore-1.jks", "password")
                .setTrustStore("client-truststore.jks", "password")
                .build();
        given().spec(spec).get("/my-path").then().body(containsString("OK"));
    }

}
