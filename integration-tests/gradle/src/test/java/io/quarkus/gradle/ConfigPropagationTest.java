package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class ConfigPropagationTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void test() throws Exception {
        File projectDir = getProjectDir("config-propagation");
        runGradleWrapper(projectDir, "test");
    }
}
