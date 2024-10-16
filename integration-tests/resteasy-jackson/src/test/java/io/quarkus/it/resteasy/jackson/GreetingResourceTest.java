package io.quarkus.it.resteasy.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import java.sql.Date;
import java.time.LocalDate;

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

        given()
                .contentType(ContentType.JSON)
                .body(greeting)
                .post("/greeting")
                .then()
                .statusCode(200)
                .body("message", equalTo("hello"),
                        "date", equalTo("2019-01-01"),
                        "sqlDate", equalTo("2019-01-01"),
                        "xmlGregorianCalendar", equalTo("2019-01-01T00:00:00.000+00:00"))
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
