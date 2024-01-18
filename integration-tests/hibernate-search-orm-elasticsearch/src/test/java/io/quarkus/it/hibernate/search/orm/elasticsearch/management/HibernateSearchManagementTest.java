package io.quarkus.it.hibernate.search.orm.elasticsearch.management;

import static org.hamcrest.Matchers.is;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.vertx.core.http.HttpHeaders;

@QuarkusTest
class HibernateSearchManagementTest {

    protected String getPrefix() {
        return "http://localhost:9001";
    }

    @Test
    void simple() {
        RestAssured.given()
                .queryParam("wait_for", "finished")
                .header(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                .post(getPrefix() + "/q/hibernate-search/reindex")
                .then().statusCode(200)
                .body(Matchers.stringContainsInOrder("Reindexing started", "Reindexing succeeded"));
    }

    @Test
    void specificTypeOnly() {
        RestAssured.when().put("/test/management/init-data").then()
                .statusCode(204);

        RestAssured.get("/test/management/search-count")
                .then().statusCode(200)
                .body(is("0"));

        RestAssured.given()
                .queryParam("wait_for", "finished")
                .header(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                .body("{\"filter\": {\"types\": [\"" + ManagementTestEntity.class.getName() + "\"]}}")
                .post(getPrefix() + "/q/hibernate-search/reindex")
                .then().statusCode(200)
                .body(Matchers.stringContainsInOrder("Reindexing started", "Reindexing succeeded"));

        RestAssured.get("/test/management/search-count")
                .then().statusCode(200)
                .body(is("5"));
    }

}
