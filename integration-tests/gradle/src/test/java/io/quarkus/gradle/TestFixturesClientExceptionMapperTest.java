package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class TestFixturesClientExceptionMapperTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {
        final File projectDir = getProjectDir("test-fixtures-client-exception-mapper");
        runGradleWrapper(projectDir, "clean", "build");
    }
}
