package io.quarkus.it.vertx;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
public class DnsTest {

    @Test
    public void testDnsResolution() {
        Response response = get("/vertx-test/dns").thenReturn();
        // If the dns resolution fails a 500 is returned.
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.asString()).isNotBlank().contains(".");
    }

}
