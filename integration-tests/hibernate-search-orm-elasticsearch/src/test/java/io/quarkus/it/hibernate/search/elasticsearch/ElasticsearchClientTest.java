package io.quarkus.it.hibernate.search.elasticsearch;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ElasticsearchClientTest {

    @Test
    public void testConnection() throws Exception {
        RestAssured.when().get("/test/elasticsearch-client/connection").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testFullCycle() throws Exception {
        RestAssured.when().get("/test/elasticsearch-client/full-cycle").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testSniffer() throws Exception {
        RestAssured.when().get("/test/elasticsearch-client/sniffer").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
