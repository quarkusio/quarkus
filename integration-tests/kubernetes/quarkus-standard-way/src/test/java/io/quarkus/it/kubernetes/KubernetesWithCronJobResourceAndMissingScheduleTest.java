
package io.quarkus.it.kubernetes;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithCronJobResourceAndMissingScheduleTest {

    static final String APP_NAME = "kubernetes-with-cronjob-resource-and-missing-schedule";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())))
            .setExpectedException(IllegalArgumentException.class);

    @Test
    public void testShouldNotBeInvokedBecauseMissingScheduleProperty() {
        Assertions.fail();
    }
}
