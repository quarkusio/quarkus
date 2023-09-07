package io.quarkus.smallrye.graphql.deployment.federation.complex;

import static org.hamcrest.Matchers.containsString;

import jakarta.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class GraphQLFederationComplexTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FooApi.class, Foo.class, Bar.class)
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
        String query = "query federation($representations: [_Any!]!) {  \n" +
                "  _entities(representations: $representations) {\n" +
                "    ... on Foo {\n" +
                "      __typename\n" +
                "      id\n" +
                "      name\n" +
                "    }\n" +
                "    ... on Bar{\n" +
                "      __typename\n" +
                "      id\n" +
                "      otherId\n" +
                "      name\n" +
                "    }\n" +
                "  }\n" +
                "}";
        String variables = "{ \n" +
                "  \"representations\": [ \n" +
                "    { \n" +
                "      \"__typename\": \"Bar\", \n" +
                "      \"id\": \"2\", \n" +
                "      \"otherId\": \"???\" \n" +
                "    }, \n" +
                "    { \n" +
                "      \"__typename\": \"Foo\", \n" +
                "      \"id\": 1\n" +
                "    }, \n" +
                "    { \n" +
                "      \"__typename\": \"Bar\", \n" +
                "      \"id\": \"3\", \n" +
                "      \"otherId\": \"!!!\" \n" +
                "    }, \n" +
                "    { \n" +
                "      \"__typename\": \"Foo\", \n" +
                "      \"id\": 0\n" +
                "    } \n" +
                "  ] \n" +
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
                        "{\"data\":{\"_entities\":[{\"__typename\":\"Bar\",\"id\":2,\"otherId\":\"???\",\"name\":\"2???\"},{\"__typename\":\"Foo\",\"id\":1,\"name\":\"Name of 1\"},{\"__typename\":\"Bar\",\"id\":3,\"otherId\":\"!!!\",\"name\":\"3!!!\"},{\"__typename\":\"Foo\",\"id\":0,\"name\":\"Name of 0\"}]}}"));
    }
}
