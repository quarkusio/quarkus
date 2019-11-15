package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.security.Provider;
import java.security.Security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.NativeImageTest;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

@NativeImageTest
public class VertxProducerResourceIT extends VertxProducerResourceTest {

    private static Provider sunECProvider;

    @BeforeAll
    public static void setupSecProvider() {
        //Remove SunEC provider for the test as it's not being provided for tests.
        sunECProvider = Security.getProvider("SunEC");
        Security.removeProvider("SunEC");
    }

    @AfterAll
    public static void restoreSecProvider() {
        Security.addProvider(sunECProvider);
    }

    @Test
    public void testRouteRegistrationMTLS() {
        RequestSpecification spec = new RequestSpecBuilder()
                .setBaseUri(String.format("%s://%s", url.getProtocol(), url.getHost()))
                .setPort(8443)
                .setKeyStore("client-keystore.jks", "password")
                .setTrustStore("client-truststore.jks", "password")
                .build();
        given().spec(spec).get("/my-path").then().body(containsString("OK"));
    }
}
