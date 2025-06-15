package io.quarkus.smallrye.graphql.deployment;

import java.util.Arrays;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CompressionTest extends AbstractGraphQLTest {

    private static final String PI = "3.141592653589793238462643383279502884197169399375105820";
    private static final String TAU = "6.28";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.http.enable-compression", "true");

    @Test
    public void singleValidQueryCompressedResponseTest() {
        String body = getPayload("{ validPiQuery }");
        assertCompressed(body, PI);
    }

    @Test
    public void multipleValidQueriesCompressedResponseTest() {
        String body = getPayload("{ validPiQuery validTauQuery }");
        assertCompressed(body, PI, TAU);
    }

    @Test
    public void singleInvalidQueryCompressedResponseTest() {
        String body = getPayload("{ invalidQuery }");
        assertCompressed(body, "errors");
    }

    private void assertCompressed(String body, String... expectedOutput) {
        org.hamcrest.Matcher messageMatcher = Arrays.stream(expectedOutput).map(CoreMatchers::containsString)
                .reduce(Matchers.allOf(), (a, b) -> Matchers.allOf(a, b));

        RestAssured.given().body(body).contentType(MEDIATYPE_JSON).post("/graphql").prettyPeek().then().assertThat()
                .statusCode(200).header("Content-Encoding", "gzip").body(messageMatcher);
    }

    @GraphQLApi
    public static class Schema {
        @Query
        public String validPiQuery() {
            return PI;
        }

        @Query
        public String validTauQuery() {
            return TAU;
        }

        @Query
        public String invalidQuery() {
            throw new RuntimeException();
        }
    }

}
