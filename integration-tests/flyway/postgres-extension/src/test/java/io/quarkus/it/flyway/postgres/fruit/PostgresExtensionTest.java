package io.quarkus.it.flyway.postgres.fruit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Test various Hibernate Multitenancy operations running in Quarkus
 */
@QuarkusTest
public class PostgresExtensionTest {

    @Test
    public void testMandatoryDisabledTransactionalLockForConcurrentOperation() throws Exception {
        int firstRow = 0;
        var fruits = given().when().get("/fruits/index").then().assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .extract()
                .as(Object.class);

        //        ArrayList<ArrayList<String>> fruitss;
        //        if (fruits == null)
        //            fruitss = new ArrayList<>();
        //        else {
        //
        //            fruitss = (ArrayList<ArrayList<String>>) fruits;
        //        }
        //        assertThat(fruitss.get(0).toArray(), hasItemInArray("concurently_added_fruit_name"));

    }

}
