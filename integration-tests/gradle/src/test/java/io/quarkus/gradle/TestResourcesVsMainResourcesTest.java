package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class TestResourcesVsMainResourcesTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void test() throws Exception {

        final File projectDir = getProjectDir("test-resources-vs-main-resources");

        runGradleWrapper(projectDir, "test");
    }
}
