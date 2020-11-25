package io.quarkus.it.jpa.jdbcmetadata;

import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JPAJdbcMetadataRetrievalTest {

    @Test
    public void test() {
        when().get("/jpa-test/jdbc-metadata-retrieval").then()
                .body(is("thePersistedName"))
                .statusCode(200);
    }

}
