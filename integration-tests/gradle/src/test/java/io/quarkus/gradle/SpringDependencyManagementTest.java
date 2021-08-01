package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

public class SpringDependencyManagementTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testQuarkusBuildShouldWorkWithSpringDependencyManagement()
            throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("spring-dependency-plugin-project");

        final BuildResult result = runGradleWrapper(projectDir, "clean", "quarkusBuild");

        assertThat(result.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }
}
