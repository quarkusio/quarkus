package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ObjectScalarTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ObjectApi.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.extra-scalars=object\n" +
                            "quarkus.smallrye-graphql.schema-include-scalars=true"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testObject() {
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
    public void testObjectSchemaDefinition() {
        RestAssured.given()
                .get("/graphql/schema.graphql")
                .prettyPeek()
                .then()
                .assertThat()
                .body(containsString("scalar Object"))
                .body(containsString("echo(input: Object): Object"))
                .statusCode(200);
    }

    @GraphQLApi
    public static class ObjectApi {

        @Query
        public Object echo(Object input) {
            return input;
        }

    }

}
