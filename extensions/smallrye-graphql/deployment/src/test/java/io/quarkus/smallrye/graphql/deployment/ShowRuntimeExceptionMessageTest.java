package io.quarkus.smallrye.graphql.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ShowRuntimeExceptionMessageTest extends AbstractGraphQLTest {

    private static final String ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE = "Something went wrong";
    private static final String ILLEGAL_STATE_EXCEPTION_MESSAGE = "Something else went wrong";
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestApi.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.smallrye-graphql.show-runtime-exception-message=" +
                                            "java.lang.IllegalArgumentException," +
                                            "java.lang.IllegalStateException"),
                            "application.properties"));

    @Test
    void testExcludeNullFieldsInResponse() {
        given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(getPayload("{ something }"))
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(OK)
                .and()
                .body(containsString(ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE));

        given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(getPayload("{ somethingElse }"))
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(OK)
                .and()
                .body(containsString(ILLEGAL_STATE_EXCEPTION_MESSAGE));
    }

    @GraphQLApi
    public static class TestApi {
        @Query
        public String getSomething() {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE);
        }

        @Query
        public String getSomethingElse() {
            throw new IllegalStateException(ILLEGAL_STATE_EXCEPTION_MESSAGE);
        }
    }
}
