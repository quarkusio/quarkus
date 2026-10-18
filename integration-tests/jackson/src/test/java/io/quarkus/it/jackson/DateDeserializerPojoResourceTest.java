package io.quarkus.it.jackson;

import static io.quarkus.it.jackson.TestUtil.getObjectMapperForTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DateDeserializerPojoResourceTest {

    @Test
    public void testSqlTimestamp() throws IOException {
        SqlTimestampPojo pojo = new SqlTimestampPojo();
        pojo.timestamp = new Timestamp(0);

        given()
                .body(getObjectMapperForTest().writeValueAsString(pojo))
                .when().post("/datedeserializers/sql/timestamp")
                .then()
                .statusCode(200)
                // Jackson 3 uses "Z" instead of "+00:00" for UTC
                .body("timestamp", equalTo("1970-01-01T00:00:00.000Z"));
    }

    @Test
    public void testSqlDate() throws IOException {
        SqlDatePojo pojo = new SqlDatePojo();
        pojo.date = new Date(0);

        given()
                .body(getObjectMapperForTest().writeValueAsString(pojo))
                .when().post("/datedeserializers/sql/date")
                .then()
                .statusCode(200)
                // Jackson 3 serializes java.sql.Date like java.util.Date (full date-time)
                // instead of using toString() (date-only). See https://github.com/FasterXML/jackson-databind/issues/2405
                .body("date", equalTo("1970-01-01T00:00:00.000Z"));
    }
}
