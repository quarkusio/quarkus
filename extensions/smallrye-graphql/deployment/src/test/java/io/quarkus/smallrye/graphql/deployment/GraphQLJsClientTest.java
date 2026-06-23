package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.CoreMatchers.containsString;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class GraphQLJsClientTest {

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
    public void testClientLibraryIsServed() {
        RestAssured.get("/_static/quarkus-graphql/graphql-client.js")
                .then()
                .statusCode(200)
                .body(containsString("export class GraphQLClient"));
    }

    @Test
    public void testTypedProxyIsServed() {
        RestAssured.get("/_static/quarkus-graphql-api/graphql-api.js")
                .then()
                .statusCode(200)
                .body(containsString("import { GraphQLClient }"))
                .body(containsString("export const client"))
                .body(containsString("export const Queries"))
                .body(containsString("export const Mutations"));
    }

    @Test
    public void testProxyContainsQueryOperations() {
        RestAssured.get("/_static/quarkus-graphql-api/graphql-api.js")
                .then()
                .statusCode(200)
                .body(containsString("ping"))
                .body(containsString("query ping"))
                .body(containsString("message"));
    }

    @Test
    public void testProxyContainsMutationOperations() {
        RestAssured.get("/_static/quarkus-graphql-api/graphql-api.js")
                .then()
                .statusCode(200)
                .body(containsString("moo"))
                .body(containsString("mutation moo"));
    }

    @Test
    public void testClientDeclarationsAreServed() {
        RestAssured.get("/_static/quarkus-graphql/graphql-client.d.ts")
                .then()
                .statusCode(200)
                .body(containsString("export class GraphQLClient"))
                .body(containsString("export class GraphQLError"))
                .body(containsString("export class Subscription"));
    }

    @Test
    public void testTypedProxyDeclarationsAreServed() {
        RestAssured.get("/_static/quarkus-graphql-api/graphql-api.d.ts")
                .then()
                .statusCode(200)
                .body(containsString("import { GraphQLClient, Subscription }"))
                .body(containsString("export declare const client: GraphQLClient"))
                .body(containsString("export declare const Queries"))
                .body(containsString("export declare const Mutations"))
                .body(containsString("export interface TestPojo"));
    }

    @Test
    public void testDeclarationsContainOperationTypes() {
        RestAssured.get("/_static/quarkus-graphql-api/graphql-api.d.ts")
                .then()
                .statusCode(200)
                .body(containsString("ping:"))
                .body(containsString("Promise<TestPojo>"))
                .body(containsString("moo:"))
                .body(containsString("name: string"));
    }

}
