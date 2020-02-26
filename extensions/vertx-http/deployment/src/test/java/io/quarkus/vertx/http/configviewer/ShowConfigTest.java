package io.quarkus.vertx.http.configviewer;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.hasItem;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.http.ContentType;

public class ShowConfigTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("foo=bar"), "application.properties"));

    @Test
    void testConfig() {
        when().get("/config").then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("sources.properties.foo", hasItem("bar"));
    }
}
