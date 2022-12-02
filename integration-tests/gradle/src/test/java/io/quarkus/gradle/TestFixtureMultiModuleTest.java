package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

public class TestFixtureMultiModuleTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testTaskShouldUseTestFixtures() throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("test-fixtures-multi-module");
        final BuildResult result = runGradleWrapper(projectDir, "clean", "test");
        assertThat(BuildResult.isSuccessful(result.getTasks().get(":application:test"))).isTrue();
    }
}
