package io.quarkus.smallrye.graphql.deployment.federation.batch;

import static org.hamcrest.Matchers.containsString;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class GraphQLFederationBatchTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FooApi.class, Foo.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.schema-include-directives=true\n" +
                            "quarkus.smallrye-graphql.federation.batch-resolving-enabled=true"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void checkServiceDeclarationInSchema() {
        RestAssured.given()
                .get("/graphql/schema.graphql")
                .then()
                .body(containsString("type _Service {"));
    }

    @Test
    public void checkFederationDirectivesInSchema() {
        RestAssured.given()
                .get("/graphql/schema.graphql")
                .then()
                .body(containsString("id: Int! @external"))
                .body(containsString("type Foo @extends @key(fields : \"id\")"));
        ;
    }

    @Test
    public void resolvePerFederation() {
        String query = "query federation($representations: [_Any!]!) {\n" +
                "    _entities(representations: $representations) {\n" +
                "        ... on Foo {\n" +
                "            id\n" +
                "            name\n" +
                "        }\n" +
                "    }\n" +
                "}";
        String variables = "{\n" +
                "  \"representations\": [\n" +
                "    {\n" +
                "      \"__typename\": \"Foo\",\n" +
                "      \"id\": 1\n" +
                "    },\n" +
                "    {\n" +
                "      \"__typename\": \"Foo\",\n" +
                "      \"id\": 2\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String request = getPayload(query, variables);
        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.is(
                        "{\"data\":{\"_entities\":[{\"id\":1,\"name\":\"Name of 1\"},{\"id\":2,\"name\":\"Name of 2\"}]}}"));
    }
}
