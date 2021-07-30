package io.quarkus.it.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.VerificationException;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(WireMockZipkin.class)
public class ZipkinIntegrationTest {

    @Test
    public void testZipkinIntegration() {

        given().when().get("/echo").then().statusCode(200).body(containsString("result"));

        boolean continues = true;
        long current = new Date().getTime();
        while (continues) {
            try {
                verify(postRequestedFor(urlEqualTo("/zipkin")));
                continues = false;
            } catch (VerificationException e) {
                // Because the request to zipkin is asynchronous we should wait maximum 10
                // seconds.
                if (new Date().getTime() - current > 10000) {
                    throw e;
                }
            }
        }

        assertEquals(0, WireMockZipkin.getWireMockServer().findAllUnmatchedRequests().size());
    }

}
