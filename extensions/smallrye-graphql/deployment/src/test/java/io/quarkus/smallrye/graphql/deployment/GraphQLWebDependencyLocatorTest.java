package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.CoreMatchers.containsString;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class GraphQLWebDependencyLocatorTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, TestPojo.class, TestPojo.Number.class,
                            TestGenericsPojo.class, TestRandom.class,
                            TestUnion.class, TestUnionMember.class,
                            BusinessException.class, CustomDirective.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.smallrye-graphql.js-client.enabled", "true");

    @Test
    public void testImportMapContainsGraphQLMappings() {
        RestAssured.get("/_importmap/generated_importmap.js")
                .then()
                .statusCode(200)
                .body(containsString("@quarkus/graphql"))
                .body(containsString("/_static/quarkus-graphql/graphql-client.js"))
                .body(containsString("@quarkus/graphql-api"))
                .body(containsString("/_static/quarkus-graphql-api/graphql-api.js"));
    }
}
