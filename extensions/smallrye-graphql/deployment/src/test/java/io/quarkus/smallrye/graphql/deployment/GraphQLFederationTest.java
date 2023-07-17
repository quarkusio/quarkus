package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.Matchers.containsString;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.graphql.api.federation.Extends;

public class GraphQLFederationTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FooApi.class, Foo.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.schema-include-directives=true"),
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
                .body(containsString("name: String @extends"));
    }

    @GraphQLApi
    static class FooApi {

        @Query
        public Foo foo() {
            return new Foo();
        }

    }

    static class Foo {

        @Extends
        public String name;

    }

}
