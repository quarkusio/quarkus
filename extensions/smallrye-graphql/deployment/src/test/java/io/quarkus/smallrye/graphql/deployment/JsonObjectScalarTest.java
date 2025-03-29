package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import jakarta.json.JsonObject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Support for the JSON graphql type represented in Java as jakarta.json.JsonObject
 */
public class JsonObjectScalarTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ObjectApi.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.extra-scalars=json\n" +
                            "quarkus.smallrye-graphql.schema-include-scalars=true"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testJsonObject() {
        String query = getPayload("{ echo(input: { field1: 1, field2: \"2\"}) }");
        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql/")
                .then()
                .assertThat()
                .statusCode(200)
                .body("data.echo.field1", equalTo(1))
                .body("data.echo.field2", equalTo("2"));
    }

    @Test
    public void testJsonSchemaDefinition() {
        RestAssured.given()
                .get("/graphql/schema.graphql")
                .prettyPeek()
                .then()
                .assertThat()
                .body(containsString("scalar JSON"))
                .body(containsString("echo(input: JSON): JSON"))
                .statusCode(200);
    }

    @GraphQLApi
    public static class ObjectApi {

        @Query
        public JsonObject echo(JsonObject input) {
            return input;
        }

    }

}
