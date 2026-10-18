package io.quarkus.it.smallrye.graphql;

import static io.quarkus.it.smallrye.graphql.PayloadCreator.MEDIATYPE_JSON;
import static io.quarkus.it.smallrye.graphql.PayloadCreator.getPayload;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingsResourceTest {

    @Test
    public void test() throws IOException {

        String loadMutation = getPayload(mutation);

        given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(loadMutation)
                .post("/graphql")
                .then()
                .statusCode(200)
                .and()
                .body(containsString("afr"))
                .body(containsString("Goeie more"))
                .body(containsString("Goeie middag"))
                .body(containsString("Goeie naand"));

    }

    private String mutation = "mutation LoadGreetings {\n" +
            "  load(greetings : \n" +
            "    {\n" +
            "      language: \"afr\",\n" +
            "      hellos:[\n" +
            "        {\n" +
            "          message:\"Goeie more\",\n" +
            "          time: \"07:00\"\n" +
            "        },\n" +
            "        {\n" +
            "      		message:\"Goeie middag\",\n" +
            "          time: \"13:00\"\n" +
            "          \n" +
            "        },\n" +
            "        {\n" +
            "          message:\"Goeie naand\",\n" +
            "          time: \"18:00\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ){\n" +
            "    language,\n" +
            "    hellos {\n" +
            "      time\n" +
            "      message\n" +
            "    }\n" +
            "  }\n" +
            "}";

}
