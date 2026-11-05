package io.quarkus.it.vertx.asyncdns;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AsyncDnsTest {

    @Test
    public void usesAsyncDnsResolver() {
        RestAssured.when().get("/dns").then()
                .statusCode(200)
                .body(equalTo("io.vertx.core.dns.impl.DnsAddressResolverProvider"));
    }
}
