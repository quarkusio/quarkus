package io.quarkus.smallrye.graphql.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;

import java.io.IOException;
import java.sql.SQLException;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class HideCheckedExceptionMessageTest extends AbstractGraphQLTest {

    private static final String IOEXCEPTION_MESSAGE = "Something went wrong";
    private static final String INTERRUPTED_EXCEPTION_MESSAGE = "Something else went wrong";
    private static final String SQL_EXCEPTION_MESSAGE = "Something went really wrong, but should expect a message";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestApi.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.smallrye-graphql.hide-checked-exception-message=" +
                                            "java.io.IOException," +
                                            "java.lang.InterruptedException"),
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
                .body(not(containsString(IOEXCEPTION_MESSAGE)));

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
                .body(not(containsString(INTERRUPTED_EXCEPTION_MESSAGE)));

        given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(getPayload("{ somethingElseElse }"))
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(OK)
                .and()
                .body(containsString(SQL_EXCEPTION_MESSAGE));
    }

    @GraphQLApi
    public static class TestApi {
        @Query
        public String getSomething() throws IOException {
            throw new IOException(IOEXCEPTION_MESSAGE);
        }

        @Query
        public String getSomethingElse() throws InterruptedException {
            throw new InterruptedException(INTERRUPTED_EXCEPTION_MESSAGE);
        }

        @Query
        public String getSomethingElseElse() throws SQLException {
            throw new SQLException(SQL_EXCEPTION_MESSAGE);
        }
    }

}
