package io.quarkus.smallrye.graphql.deployment.federation;

import static com.apollographql.federation.graphqljava.tracing.FederatedTracingInstrumentation.FEDERATED_TRACING_HEADER_NAME;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class GraphQLFederationTracingTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FooApi.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.federation.enabled=true"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void resolvePerFederation() {
        String TEST_QUERY = "query foo { foo }";

        String ftKey = "ftv1";

        String request = getPayload(TEST_QUERY);
        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .header(FEDERATED_TRACING_HEADER_NAME, ftKey)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(
                        "{\"data\":{\"foo\":\"foo\"}"))
                .and()
                .body(CoreMatchers.containsString("\"extensions\":{\"" + ftKey + "\":\""));
    }

    @GraphQLApi
    public static class FooApi {
        @Query
        public String getFoo() {
            return "foo";
        }
    }

}
