package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class TestPrefixedProfilePropertiesTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void test() throws Exception {
        File projectDir = getProjectDir("test-prefixed-profile-properties");
        runGradleWrapper(projectDir, "test");
    }
}
