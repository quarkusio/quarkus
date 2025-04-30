package io.quarkus.smallrye.graphql.deployment.federation.resolver;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class FederationResolverTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ExtendedApi.class, ExtendedType.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.schema-include-directives=true"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void resolverById() {
        String request = getPayload(TEST_ID_QUERY);
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
                        "{\"data\":{\"_entities\":[{\"id\":\"id\",\"description\":\"extendedTypeById\"}]}}"));
    }

    @Test
    public void resolverByIdNameKey() {
        String request = getPayload(TEST_ID_NAME_KEY_QUERY);
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
                        "{\"data\":{\"_entities\":[{\"id\":\"id\",\"value\":\"idnamekey\",\"description\":\"extendedTypeByIdNameKey\"}]}}"));
    }

    private static final String TEST_ID_QUERY = "query {\n" +
            "_entities(\n" +
            "    representations: { id: \"id\", __typename: \"ExtendedType\" }\n" +
            ") {\n" +
            "    ... on ExtendedType {\n" +
            "        id\n" +
            "        description\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String TEST_ID_NAME_KEY_QUERY = "query {\n" +
            "_entities(\n" +
            "    representations: { id: \"id\", name: \"name\", key: \"key\", __typename: \"ExtendedType\" }\n" +
            ") {\n" +
            "    ... on ExtendedType {\n" +
            "        id\n" +
            "        value\n" +
            "        description\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
