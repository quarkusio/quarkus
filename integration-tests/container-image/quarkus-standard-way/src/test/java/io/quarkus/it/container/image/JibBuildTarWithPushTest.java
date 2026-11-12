package io.quarkus.it.container.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;

public class JibBuildTarWithPushTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("jib-build-tar-with-push")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.container-image.group", "quarkus")
            .overrideConfigKey("quarkus.container-image.build", "true")
            .overrideConfigKey("quarkus.container-image.push", "true")
            .overrideConfigKey("quarkus.container-image.tar.enabled", "true")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-container-image-jib", Version.getVersion())))
            .assertBuildException(e -> assertThat(e).hasStackTraceContaining("quarkus.container-image.tar.enabled"));

    @Test
    public void testBuildFails() {
        // the assertion is done on the build exception
    }
}
