package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class IntegrationTestProfileConfigLocationsTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void test() throws Exception {
        File projectDir = getProjectDir("integration-test-profile-config-locations");
        runGradleWrapper(projectDir, "clean", "quarkusIntTest");
    }
}
