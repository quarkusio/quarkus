package io.quarkus.it.smallrye.graphql;

import static io.quarkus.it.smallrye.graphql.PayloadCreator.MEDIATYPE_JSON;
import static io.quarkus.it.smallrye.graphql.PayloadCreator.getPayload;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ExtendedScalarsResourceTest {

    @Test
    void testEndpoint() {

        String listRequest = getPayload("{\n" +
                "  scalars {\n" +
                "    url\n" +
                "    locale\n" +
                "    countryCode\n" +
                "    currency\n" +
                "  }\n" +
                "}");

        given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(listRequest)
                .post("/graphql")
                .then()
                .statusCode(200)
                .and()
                .body("data.scalars.size()", is(0));

        String mutationRequest = getPayload(mutation);

        given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(mutationRequest)
                .post("/graphql")
                .then()
                .log().all()
                .statusCode(200)
                .and()
                .body("data.scalarsAdd.url", is("https://www.quarkus.io"))
                .body("data.scalarsAdd.locale", is("en_AU"))
                .body("data.scalarsAdd.countryCode", is("AU"))
                .body("data.scalarsAdd.currency", is("AUD"));
    }

    private String mutation = "mutation scalarsAdd {\n" +
            "  scalarsAdd(scalar : \n" +
            "    {\n" +
            "      url: \"https://www.quarkus.io\",\n" +
            "      locale: \"en-AU\",\n" +
            "      countryCode: \"AU\"\n" +
            "      currency: \"AUD\"\n" +
            "    }\n" +
            "  ){\n" +
            "    url,\n" +
            "    locale,\n" +
            "    countryCode,\n" +
            "    currency\n" +
            "  }\n" +
            "}";

}
