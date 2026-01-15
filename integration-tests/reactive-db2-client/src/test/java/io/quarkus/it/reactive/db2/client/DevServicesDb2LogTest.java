package io.quarkus.it.reactive.db2.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that Dev Services for DB2 correctly handles datasource names
 * longer than 8 characters (DB2's database name limit).
 * <p>
 * The "additional" datasource (10 characters) exceeds the limit and
 * triggers automatic truncation. This test verifies the truncated
 * database name works correctly.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51225">GitHub Issue #51225</a>
 */
@QuarkusTest
public class DevServicesDb2LogTest {

    @Test
    public void testLongDatasourceNameWorks() {
        given()
                .when().get("/plants/legumes/")
                .then()
                .statusCode(200)
                .body(containsString("Broccoli"));
    }
}
