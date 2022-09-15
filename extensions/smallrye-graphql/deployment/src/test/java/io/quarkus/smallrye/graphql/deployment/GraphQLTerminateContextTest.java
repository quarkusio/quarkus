package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.MEDIATYPE_JSON;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Testing that the context terminate
 */
public class GraphQLTerminateContextTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(TestTerminateContextResource.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testWhoAmI() {
        runTestWith("user1");
        runTestWith("user2");
        runTestWith("user2");
    }

    public void runTestWith(String expectedUser) {
        String fooRequest = getPayload("{\n" +
                "  whoami {\n" +
                "    name\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(fooRequest)
                .with().header("X-Test", expectedUser)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .log().body().and()
                .body("data.whoami.name", Matchers.equalTo(expectedUser));
    }

    @GraphQLApi
    public static class TestTerminateContextResource {

        @Inject
        RoutingContext ctx;

        @Query
        public Uni<YouAre> whoami() {
            return Uni.createFrom().item(() -> {
                YouAre youAre = new YouAre();
                youAre.name = ctx.request().headers().get("X-Test");
                return youAre;
            });
        }
    }

    public static class YouAre {
        public String name;
    }
}
