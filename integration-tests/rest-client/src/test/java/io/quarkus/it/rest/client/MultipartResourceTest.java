package io.quarkus.it.rest.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.DisabledOnNativeImage;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.restclient.RestClientTestSupport;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTest
public class MultipartResourceTest {

    @Order(3) // execute this last to make sure that the reset of the base URL is properly performed automatically
    @Test
    public void testMultipartDataIsSent() {
        given()
                .header("Content-Type", "text/plain")
                .when().post("/client/multipart")
                .then()
                .statusCode(200)
                .body(containsString("Content-Disposition: form-data; name=\"file\""),
                        containsString("HELLO WORLD"),
                        containsString("Content-Disposition: form-data; name=\"fileName\""),
                        containsString("greeting.txt"));
    }

    @DisabledOnNativeImage
    @Order(1)
    @Test
    public void testCustomEcho() throws URISyntaxException {
        RestClientTestSupport.setBaseURI(MultipartService.class, new URI(System.getProperty("test.url") + "/other"));
        given()
                .header("Content-Type", "text/plain")
                .when().post("/client/multipart")
                .then()
                .statusCode(200)
                .body(containsString("other"));
    }

    @DisabledOnNativeImage
    @Order(2)
    @Test
    public void testAnotherCustomEcho() throws URISyntaxException {
        RestClientTestSupport.setBaseURI(MultipartService.class, new URI(System.getProperty("test.url") + "/another/new"));
        given()
                .header("Content-Type", "text/plain")
                .when().post("/client/multipart")
                .then()
                .statusCode(200)
                .body(containsString("another"));
    }

}
