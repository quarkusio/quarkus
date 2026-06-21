package io.quarkus.it.hibernate.orm.cache;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HibernateOrmCacheFunctionalityTest {

    @Test
    public void testCacheJPAFunctionalityFromServlet() {
        RestAssured.when().get("/hibernate-orm-cache/testfunctionality").then().body(is("OK"));
    }

    @Test
    public void testEntityMemoryObjectCountOverride() {
        RestAssured.when()
                .get("/hibernate-orm-cache/memory-object-count/com.example.EntityA")
                .then().body(is("200"));
    }

    @Test
    public void testEntityExpirationMaxIdleOverride() {
        RestAssured.when()
                .get("/hibernate-orm-cache/expiration-max-idle/com.example.EntityB")
                .then().body(is("86400"));
    }

}
