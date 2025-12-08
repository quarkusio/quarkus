package io.quarkus.smallrye.graphql.deployment;

import static io.restassured.RestAssured.given;

import java.net.HttpURLConnection;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GraphQLOverHttpDisabledGetTest extends AbstractGraphQLTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
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
