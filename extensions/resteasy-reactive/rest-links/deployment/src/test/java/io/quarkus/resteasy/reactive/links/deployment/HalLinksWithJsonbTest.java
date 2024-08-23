package io.quarkus.resteasy.reactive.links.deployment;

import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;

public class HalLinksWithJsonbTest extends AbstractHalLinksTest {
    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractId.class, AbstractEntity.class, TestRecord.class, TestResource.class,
                            TestRecordWithIdAndPersistenceIdAndRestLinkId.class, TestRecordWithIdAndRestLinkId.class,
                            TestRecordWithIdAndPersistenceId.class, TestRecordWithPersistenceId.class,
                            TestRecordWithRestLinkId.class, TestRecordWithPersistenceIdAndRestLinkId.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-rest-jsonb", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-hal", Version.getVersion())))
            .setRun(true);

}
