package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.MEDIATYPE_JSON;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Basic tests for Mutation
 */
public class GraphQLMutationTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MutationApi.class, BusinessError.class)
                    .addAsResource(new StringAsset(getPropertyAsString()), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testMutation() {
        String request = getPayload("mutation hello{\n" + "  hello\n" + "}");

        RestAssured.given().when().accept(MEDIATYPE_JSON).contentType(MEDIATYPE_JSON).body(request).post("/graphql")
                .then().assertThat().statusCode(200).and().body(CoreMatchers.containsString("Hello Phillip"));

    }

    @Test
    public void testError() {
        String request = getPayload("mutation error{\n" + "  error\n" + "}");

        RestAssured.given().when().accept(MEDIATYPE_JSON).contentType(MEDIATYPE_JSON).body(request).post("/graphql")
                .then().assertThat().statusCode(200).and().body(CoreMatchers.containsString("Some error"));
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
