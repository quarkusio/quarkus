package io.quarkus.it.resteasy.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import java.sql.Date;
import java.time.LocalDate;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class GreetingResourceTest {

    @Test
    void testEndpoint() throws DatatypeConfigurationException {

        final LocalDate localDate = LocalDate.of(2019, 01, 01);
        final Date sqlDate = new Date(119, 0, 1);
        final XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar("2019-01-01T00:00:00.000+00:00");
        final Greeting greeting = new Greeting("hello", localDate, sqlDate, xmlGregorianCalendar);

        // Jackson 3 serializes java.sql.Date as full date-time in UTC
        // instead of date-only toString(). See https://github.com/FasterXML/jackson-databind/issues/2405
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String expectedSqlDate = sdf.format(sqlDate);

        given()
                .contentType(ContentType.JSON)
                .body(greeting)
                .post("/greeting")
                .then()
                .statusCode(200)
                .body("message", equalTo("hello"),
                        "date", equalTo("2019-01-01"),
                        // Jackson 3 serializes java.sql.Date as full date-time
                        "sqlDate", equalTo(expectedSqlDate),
                        // Jackson 3 uses "Z" instead of "+00:00" for UTC
                        "xmlGregorianCalendar", equalTo("2019-01-01T00:00:00.000Z"))
                .extract().body().asString();
    }

    @Test
    void testConfig() {
        // test that configuration can be obtained from application.properties
        given()
                .when().get("/greeting/config")
                .then()
                .statusCode(200)
                .body(containsString("5000"));
    }

    @Test
    public void testAbstract() {
        given()
                .when().get("/abstract/inherited")
                .then()
                .statusCode(200)
                .body(containsString("concrete"));

    }

}
