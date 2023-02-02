package io.quarkus.smallrye.graphql.deployment;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Basic tests for Mutation
 */
public class GraphQLMutationTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MutationApi.class, BusinessError.class)
                    .addAsResource(new StringAsset(getPropertyAsString()), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testMutation(String contentType) {
        String request = getPayload("mutation hello{\n" +
                "  hello\n" +
                "}");

        post("/graphql", contentType, request)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("Hello Phillip"));

    }

    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testError(String contentType) {
        String request = getPayload("mutation error{\n" +
                "  error\n" +
                "}");

        post("/graphql", contentType, request)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("Some error"));
    }

    @GraphQLApi
    public static class MutationApi {

        @Query
        public String ping() {
            return "pong";
        }

        @Mutation
        public String hello(@DefaultValue("Phillip") String name) {
            return "Hello " + name;
        }

        @Mutation
        public String error(@DefaultValue("Phillip") String name) throws BusinessError {
            throw new BusinessError("Some error");
        }
    }

    public static class BusinessError extends Exception {

        public BusinessError() {
        }

        public BusinessError(String message) {
            super(message);
        }

        public BusinessError(String message, Throwable cause) {
            super(message, cause);
        }

        public BusinessError(Throwable cause) {
            super(cause);
        }

        public BusinessError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

    }
}
