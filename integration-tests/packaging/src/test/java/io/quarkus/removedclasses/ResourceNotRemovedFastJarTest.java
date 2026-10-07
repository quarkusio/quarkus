package io.quarkus.removedclasses;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

/**
 * Baseline test verifying that the resource is accessible when not removed.
 */
public class ResourceNotRemovedFastJarTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .add(new StringAsset("quarkus.package.jar.type=fast-jar"),
                            "application.properties"))
            .setApplicationName("resource-not-removed-test")
            .setApplicationVersion(Version.getVersion())
            .setRun(true);

    @Test
    public void testResourceIsAccessibleWhenNotRemoved() {
        RestAssured.get("/shared/removed-resource").then()
                .statusCode(200)
                .body(containsString("resource is used to test"));
    }
}
