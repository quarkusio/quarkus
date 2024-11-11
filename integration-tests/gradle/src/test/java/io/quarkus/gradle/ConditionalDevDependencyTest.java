package io.quarkus.gradle;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.utils.BuildToolHelper;

public class ConditionalDevDependencyTest extends QuarkusGradleTestBase {

    @Test
    public void test() throws Exception {

        var appModel = BuildToolHelper.enableGradleAppModelForDevMode(getProjectDir("basic-java-application-project").toPath());
        appModel.getDependencies().forEach(e -> System.out.println(e.toCompactCoords()));
    }
}
