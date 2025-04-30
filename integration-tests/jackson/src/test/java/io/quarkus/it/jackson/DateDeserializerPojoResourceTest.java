package io.quarkus.it.jackson;

import static io.quarkus.it.jackson.TestUtil.getObjectMapperForTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.ZoneId;

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
                .body("timestamp", equalTo("1970-01-01T00:00:00.000+00:00"));
    }

    @Test
    public void testSqlDate() throws IOException {
        SqlDatePojo pojo = new SqlDatePojo();
        Date sqlDate = new Date(0);
        pojo.date = sqlDate;
        // the date will pass through Jackson's incorrect conversion; here is our equivalent:
        sqlDate = new Date(sqlDate.toLocalDate().atStartOfDay(ZoneId.of("UTC")).toEpochSecond() * 1000L);

        given()
                .body(getObjectMapperForTest().writeValueAsString(pojo))
                .when().post("/datedeserializers/sql/date")
                .then()
                .statusCode(200)
                .body("date", equalTo(sqlDate.toString()));
    }
}
