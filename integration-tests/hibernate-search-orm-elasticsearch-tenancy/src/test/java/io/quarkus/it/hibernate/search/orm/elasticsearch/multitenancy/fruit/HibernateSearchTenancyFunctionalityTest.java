package io.quarkus.it.hibernate.search.orm.elasticsearch.multitenancy.fruit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 * Test various Hibernate Search operations with multitenancy enabled
 */
@QuarkusTest
public class HibernateSearchTenancyFunctionalityTest {
    public static final TypeRef<List<Fruit>> FRUIT_LIST_TYPE_REF = new TypeRef<>() {
    };

    private static RestAssuredConfig config;

    @Test
    public void test() {
        String tenant1Id = "company1";
        String tenant2Id = "company2";
        String fruitName = "myFruit";

        // Check the indexes are empty
        assertThat(search(tenant1Id, fruitName)).isEmpty();
        assertThat(search(tenant2Id, fruitName)).isEmpty();

        // Create fruit for tenant 1
        Fruit fruit1 = new Fruit(fruitName);
        create(tenant1Id, fruit1);
        assertThat(search(tenant1Id, fruitName)).hasSize(1);
        assertThat(search(tenant2Id, fruitName)).isEmpty();

        // Create fruit for tenant 2
        Fruit fruit2 = new Fruit(fruitName);
        create(tenant2Id, fruit2);
        assertThat(search(tenant1Id, fruitName)).hasSize(1);
        assertThat(search(tenant2Id, fruitName)).hasSize(1);

        // Update fruit for tenant 1
        fruit1 = search(tenant1Id, fruitName).get(0);
        fruit1.setName("newName");
        update(tenant1Id, fruit1);
        assertThat(search(tenant1Id, fruitName)).isEmpty();
        assertThat(search(tenant1Id, "newName")).hasSize(1);
        assertThat(search(tenant2Id, fruitName)).hasSize(1);
        assertThat(search(tenant2Id, "newName")).isEmpty();

        // Delete fruit for tenant 2
        fruit2 = search(tenant2Id, fruitName).get(0);
        delete(tenant2Id, fruit2);
        assertThat(search(tenant1Id, fruitName)).isEmpty();
        assertThat(search(tenant2Id, fruitName)).isEmpty();
    }

    private void create(String tenantId, Fruit fruit) {
        given().config(config).with().body(fruit).contentType(ContentType.JSON)
                .when().post("/" + tenantId + "/fruits")
                .then()
                .statusCode(is(Status.CREATED.getStatusCode()));
    }

    private void update(String tenantId, Fruit fruit) {
        given().config(config).with().body(fruit).contentType(ContentType.JSON)
                .when().put("/" + tenantId + "/fruits/" + fruit.getId())
                .then()
                .statusCode(is(Status.OK.getStatusCode()));
    }

    private void delete(String tenantId, Fruit fruit) {
        given().config(config)
                .when().delete("/" + tenantId + "/fruits/" + fruit.getId())
                .then()
                .statusCode(is(Status.NO_CONTENT.getStatusCode()));
    }

    private List<Fruit> search(String tenantId, String terms) {
        Response response = given().config(config)
                .when().get("/" + tenantId + "/fruits/search?terms={terms}", terms);
        if (response.getStatusCode() == Status.OK.getStatusCode()) {
            return response.as(FRUIT_LIST_TYPE_REF);
        }
        return List.of();
    }

}
