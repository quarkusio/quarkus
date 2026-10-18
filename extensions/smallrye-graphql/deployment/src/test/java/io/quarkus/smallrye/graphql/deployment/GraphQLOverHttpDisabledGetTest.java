package io.quarkus.smallrye.graphql.deployment;

import static io.restassured.RestAssured.given;

import java.net.HttpURLConnection;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class GraphQLOverHttpDisabledGetTest extends AbstractGraphQLTest {
    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(GraphQLOverHttpApi.class, User.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.http.get.enabled=false"), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void shouldNotAllowGet() {
        given().basePath("graphql")
                .contentType("application/json")
                .get()
                .then().assertThat()
                .statusCode(HttpURLConnection.HTTP_BAD_METHOD);
    }
}
