package io.quarkus.smallrye.graphql.deployment.federation;

import static org.hamcrest.Matchers.*;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Make sure that if no Federation related annotations are in the application, then Federation is
 * disabled (unless explicitly enabled in the config).
 */
public class GraphQLFederationDisabledTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FooApi.class, Foo.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void checkSchemaDoesNotIncludeServiceDeclaration() {
        RestAssured.given()
                .get("/graphql/schema.graphql")
                .then()
                .body(not(containsString("type _Service {")));
    }

    @GraphQLApi
    static class FooApi {

        @Query
        public Foo foo() {
            return new Foo();
        }

    }

    static class Foo {

        public String name;

    }

}
