package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.getPropertyAsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.NonBlocking;

public class InstrumentationTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(FooApi.class, Foo.class)
                    .addAsResource(new StringAsset(getPropertyAsString(configuration())), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testQueryDepth() {
        String query = getPayload("{ foo { nestedFoo { nestedFoo { message}}}}");
        RestAssured.given().body(query).contentType(MEDIATYPE_JSON).post("/graphql/").then().log().all().assertThat()
                .body("errors[0].message", equalTo("maximum query depth exceeded 4 > 1")).body("data", nullValue());
    }

    private static Map<String, String> configuration() {
        Map<String, String> m = new HashMap<>();
        m.put("quarkus.smallrye-graphql.events.enabled", "true");
        m.put("quarkus.smallrye-graphql.instrumentation-query-depth", "1");
        return m;
    }

    public static class Foo {

        private String message;

        public Foo(String foo) {
            this.message = foo;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }

    @GraphQLApi
    public static class FooApi {

        @Query
        @NonBlocking
        public Foo foo() {
            return new Foo("foo");
        }

        public Foo nestedFoo(@Source Foo foo) {
            return new Foo("foo");
        }
    }

}
