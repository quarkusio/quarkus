package io.quarkus.gradle;


import java.io.File;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;


public class InjectBeanFromTestConfigTest extends QuarkusGradleTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("inject-bean-from-test-config");

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("clean", ":application:test"))
                .withProjectDir(projectDir)
                .build();
    }
}
