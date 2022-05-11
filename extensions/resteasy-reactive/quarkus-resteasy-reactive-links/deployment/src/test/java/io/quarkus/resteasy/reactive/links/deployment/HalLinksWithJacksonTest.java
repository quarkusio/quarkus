package io.quarkus.resteasy.reactive.links.deployment;

import java.util.Arrays;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.test.QuarkusProdModeTest;

public class HalLinksWithJacksonTest extends AbstractHalLinksTest {
    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractEntity.class, TestRecord.class, TestResource.class))
            .setForcedDependencies(
                    Arrays.asList(
                            new AppArtifact("io.quarkus", "quarkus-resteasy-reactive-jackson", "999-SNAPSHOT"),
                            new AppArtifact("io.quarkus", "quarkus-hal", "999-SNAPSHOT")))
            .setRun(true);
}
