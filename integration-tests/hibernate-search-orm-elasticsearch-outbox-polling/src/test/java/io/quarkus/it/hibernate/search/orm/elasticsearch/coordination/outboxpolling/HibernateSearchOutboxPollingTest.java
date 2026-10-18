package io.quarkus.it.hibernate.search.orm.elasticsearch.coordination.outboxpolling;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HibernateSearchOutboxPollingTest {

    @Test
    public void testSearch() {
        // If agents are running, we know we are actually using the outbox-polling coordination strategy
        RestAssured.when().put("/test/hibernate-search-outbox-polling/check-agents-running").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().put("/test/hibernate-search-outbox-polling/init-data").then()
                .statusCode(204);

        RestAssured.when().put("/test/hibernate-search-outbox-polling/await-event-processing").then()
                .statusCode(204);

        RestAssured.when().put("/test/hibernate-search-outbox-polling/refresh").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().get("/test/hibernate-search-outbox-polling/search").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().put("/test/hibernate-search-outbox-polling/purge").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().put("/test/hibernate-search-outbox-polling/refresh").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().get("/test/hibernate-search-outbox-polling/search-empty").then()
                .statusCode(200);

        // Mass indexing involves additional steps when using outbox-polling;
        // let's just check it doesn't fail.
        RestAssured.when().put("/test/hibernate-search-outbox-polling/mass-indexer").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().get("/test/hibernate-search-outbox-polling/search").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
