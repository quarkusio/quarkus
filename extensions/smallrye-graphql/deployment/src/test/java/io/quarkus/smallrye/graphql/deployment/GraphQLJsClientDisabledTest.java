package io.quarkus.smallrye.graphql.deployment;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class GraphQLJsClientDisabledTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, TestPojo.class, TestPojo.Number.class,
                            TestGenericsPojo.class, TestRandom.class,
                            TestUnion.class, TestUnionMember.class,
                            BusinessException.class, CustomDirective.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testClientLibraryNotServedWhenDisabled() {
        RestAssured.get("/_static/quarkus-graphql/graphql-client.js")
                .then()
                .statusCode(404);
    }

    @Test
    public void testTypedProxyNotServedWhenDisabled() {
        RestAssured.get("/_static/quarkus-graphql-api/graphql-api.js")
                .then()
                .statusCode(404);
    }
}
