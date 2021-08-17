package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

public class DependencyResolutionTest extends QuarkusGradleWrapperTestBase {
    @Test
    public void shouldResolveDependencyVersionFromSuperConfigurationProject()
            throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("configuration-inheritance-project");

        final BuildResult result = runGradleWrapper(projectDir, "clean", "quarkusBuild");

        assertThat(result.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }
}
