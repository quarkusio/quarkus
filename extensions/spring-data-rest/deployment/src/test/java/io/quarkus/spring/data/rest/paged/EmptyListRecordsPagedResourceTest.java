package io.quarkus.spring.data.rest.paged;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.spring.data.rest.AbstractEntity;
import io.quarkus.test.QuarkusUnitTest;

class EmptyListRecordsPagedResourceTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractEntity.class, EmptyListRecord.class, EmptyListRecordsRepository.class)
                    .addAsResource("application.properties"));

    @Test
    void shouldListEmptyTable() {
        given().accept("application/json")
                .when().get("/empty-list-records")
                .then().statusCode(200);
    }
}
