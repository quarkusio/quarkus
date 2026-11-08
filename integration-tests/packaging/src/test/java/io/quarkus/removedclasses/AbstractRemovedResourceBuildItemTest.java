package io.quarkus.removedclasses;

import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;

import io.quarkus.builder.BuildContext;
import io.quarkus.builder.Version;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.ProdModeTestBuildStep;
import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

public abstract class AbstractRemovedResourceBuildItemTest {

    protected static QuarkusProdModeTest application(String packageType) {
        return new QuarkusProdModeTest()
                .withApplicationRoot((jar) -> jar
                        .add(new StringAsset("quarkus.package.jar.type=" + packageType),
                                "application.properties"))
                .setApplicationName("removed-resource-builditem-test")
                .setApplicationVersion(Version.getVersion())
                .addBuildChainCustomizerEntries(
                        new QuarkusProdModeTest.BuildChainCustomizerEntry(
                                RemoveResourceBuildStep.class,
                                List.of(RemovedResourceBuildItem.class),
                                Collections.emptyList()))
                .setRun(true);
    }

    @Test
    public void testResourceRemovedViaBuildItem() {
        RestAssured.get("/shared/removed-resource").then()
                .statusCode(200)
                .body(is("not found"));
    }

    @Test
    public void testOtherEndpointsStillWork() {
        RestAssured.get("/shared").then().statusCode(200);
    }

    public static class RemoveResourceBuildStep extends ProdModeTestBuildStep {

        public RemoveResourceBuildStep(Map<String, Object> testContext) {
            super(testContext);
        }

        @Override
        public void execute(BuildContext context) {
            context.produce(new RemovedResourceBuildItem(
                    ArtifactKey.of("io.quarkus", "quarkus-integration-test-shared-library"),
                    Set.of("io/quarkus/it/shared/shared-resource-for-removal-test.txt")));
        }
    }
}
