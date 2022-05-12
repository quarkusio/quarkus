package io.quarkus.resteasy.links.deployment;

import java.util.Arrays;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.QuarkusProdModeTest;

public class HalLinksWithJsonbTest extends AbstractHalLinksTest {
    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractEntity.class, TestRecord.class, TestResource.class))
            .setForcedDependencies(
                    Arrays.asList(
                            new AppArtifact("io.quarkus", "quarkus-resteasy-jsonb", Version.getVersion()),
                            new AppArtifact("io.quarkus", "quarkus-hal", Version.getVersion())))
            .setRun(true);

}
