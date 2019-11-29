package io.quarkus.vertx.graphql.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ServingUIFromCustomPathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("quarkus.vertx-graphql.ui.path=/custom\n"), "application.properties"));

    @Test
    public void shouldServeVertxGraphqlUiFromCustomPath() {
        RestAssured.when().get("/custom").then().statusCode(200);
    }
}
