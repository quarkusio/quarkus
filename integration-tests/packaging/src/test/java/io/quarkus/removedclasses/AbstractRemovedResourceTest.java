package io.quarkus.removedclasses;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;

import io.quarkus.builder.Version;
import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

public abstract class AbstractRemovedResourceTest {

    protected static QuarkusProdModeTest application(String packageType) {
        return new QuarkusProdModeTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                        .add(new StringAsset(
                                "quarkus.class-loading.removed-resources.\"io.quarkus\\:quarkus-integration-test-shared-library\"=io/quarkus/it/shared/RemovedResource.class\n"
                                        +
                                        "quarkus.package.type=" + packageType),
                                "application.properties"))
                .setApplicationName("no-paging-test")
                .setApplicationVersion(Version.getVersion())
                .setRun(true);
    }

    @Test
    public void test() {
        RestAssured.get("/removed").then().statusCode(404);
        RestAssured.get("/shared").then().statusCode(200);
    }

}
