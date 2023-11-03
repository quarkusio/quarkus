package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.*;

import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

public class GraphQLBeanValidationTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(ApiWithValidation.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @GraphQLApi
    public static class ApiWithValidation {

        @Query
        @NonBlocking
        public String nonBlocking(@Size(max = 4, message = "Message too long") String input) {
            return input;
        }

        @Query
        @Blocking
        public String blocking(@Size(max = 4, message = "Message too long") String input) {
            return input;
        }

        @Query
        @NonBlocking
        public Uni<String> uniNonBlocking(@Size(max = 4, message = "Message too long") String input) {
            return Uni.createFrom().item(input);
        }

        @Query
        @Blocking
        public Uni<String> uniBlocking(@Size(max = 4, message = "Message too long") String input) {
            return Uni.createFrom().item(input);
        }
    }

    @Test
    public void testNonBlocking() {
        String query = getPayload("{nonBlocking(input:\"TOO LONG\")}");
        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .prettyPeek()
                .then()
                .assertThat()
                .statusCode(200)
                .body("errors[0].message", containsString("Message too long"))
                .body("errors[0].path", hasItems("nonBlocking", "input"))
                .body("data.nonBlocking", nullValue());
    }

    @Test
    public void testBlocking() {
        String query = getPayload("{blocking(input:\"TOO LONG\")}");
        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .prettyPeek()
                .then()
                .assertThat()
                .statusCode(200)
                .body("errors[0].message", containsString("Message too long"))
                .body("errors[0].path", hasItems("blocking", "input"))
                .body("data.blocking", nullValue());
    }

    @Test
    public void testUniNonBlocking() {
        String query = getPayload("{uniNonBlocking(input:\"TOO LONG\")}");
        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .prettyPeek()
                .then()
                .assertThat()
                .statusCode(200)
                .body("errors[0].message", containsString("Message too long"))
                .body("errors[0].path", hasItems("uniNonBlocking", "input"))
                .body("data.uniNonBlocking", nullValue());
    }

    @Test
    public void testUniBlocking() {
        String query = getPayload("{uniBlocking(input:\"TOO LONG\")}");
        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .prettyPeek()
                .then()
                .assertThat()
                .statusCode(200)
                .body("errors[0].message", containsString("Message too long"))
                .body("errors[0].path", hasItems("uniBlocking", "input"))
                .body("data.uniBlocking", nullValue());
    }

}
