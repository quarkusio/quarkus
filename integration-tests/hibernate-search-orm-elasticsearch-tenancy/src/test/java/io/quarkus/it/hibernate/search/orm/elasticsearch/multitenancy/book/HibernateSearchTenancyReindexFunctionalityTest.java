package io.quarkus.it.hibernate.search.orm.elasticsearch.multitenancy.book;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response.Status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.http.HttpHeaders;

@QuarkusTest
@TestProfile(HibernateSearchTenancyReindexFunctionalityTest.Profile.class)
class HibernateSearchTenancyReindexFunctionalityTest {
    public static final TypeRef<List<Book>> BOOK_LIST_TYPE_REF = new TypeRef<>() {
    };

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.management.enabled", "true",
                    "quarkus.hibernate-search-orm.management.enabled", "true");
        }
    }

    @Test
    void test() {
        String tenant1Id = "company3";
        String tenant2Id = "company4";
        String bookName = "myBook";

        Book book1 = new Book(bookName);
        create(tenant1Id, book1);
        assertThat(search(tenant1Id, bookName)).isEmpty();
        Book book2 = new Book(bookName);
        create(tenant2Id, book2);
        assertThat(search(tenant2Id, bookName)).isEmpty();

        RestAssured.given()
                .queryParam("wait_for", "finished")
                .queryParam("persistence_unit", "books")
                .header(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                .body("{\"filter\": {\"tenants\": [\"" + tenant1Id + "\"], \"types\": [\"" + Book.class.getName() + "\"]}}")
                .post("http://localhost:9001/q/hibernate-search/reindex")
                .then().statusCode(200)
                .body(Matchers.stringContainsInOrder("Reindexing started", "Reindexing succeeded"));
        assertThat(search(tenant1Id, bookName)).hasSize(1);
        assertThat(search(tenant2Id, bookName)).isEmpty();
    }

    private void create(String tenantId, Book book) {
        BookTenantResolver.TENANT_ID.set(tenantId);
        given().with().body(book).contentType(ContentType.JSON)
                .when().post("/books")
                .then()
                .statusCode(is(Status.CREATED.getStatusCode()));
    }

    private List<Book> search(String tenantId, String terms) {
        BookTenantResolver.TENANT_ID.set(tenantId);

        Response response = given()
                .when().get("/books/search?terms={terms}", terms);
        if (response.getStatusCode() == Status.OK.getStatusCode()) {
            return response.as(BOOK_LIST_TYPE_REF);
        }
        return List.of();
    }

}
