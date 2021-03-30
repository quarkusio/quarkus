package io.quarkus.it.panache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.it.panache.hibernate.HibernateBook;
import io.quarkus.it.panache.mongodb.MongoBook;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various Panache operations running in Quarkus
 */
@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@QuarkusTestResource(MongoReplicaSetTestResource.class)
public class PanacheFunctionalityTest {

    @Test
    public void testHibernateFunctionality() throws Exception {
        List<HibernateBook> books = RestAssured
                .when()
                .get("/book/hibernate/bookname/bookauthor")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", HibernateBook.class);

        assertThat(books).hasSize(1);
    }

    @Test
    public void testMongoFunctionality() throws Exception {
        List<MongoBook> books = RestAssured
                .when()
                .get("/book/mongo/bookname/bookauthor")
                .then()
                .extract()
                .body()
                .jsonPath()
                .getList(".", MongoBook.class);

        assertThat(books).hasSize(1);
    }
}
