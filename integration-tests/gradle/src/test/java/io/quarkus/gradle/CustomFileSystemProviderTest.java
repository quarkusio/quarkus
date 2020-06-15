package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class CustomFileSystemProviderTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void test() throws Exception {

        final File projectDir = getProjectDir("custom-filesystem-provider");

        runGradleWrapper(projectDir, "clean", ":application:test");
    }
}
