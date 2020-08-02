package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class InjectBeanFromTestConfigTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("inject-bean-from-test-config");

        runGradleWrapper(projectDir, "clean", ":application:test");
    }
}
