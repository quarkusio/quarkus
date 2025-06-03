package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

public class TestFixtureModuleWithPackagePrivateParentTest extends QuarkusGradleWrapperTestBase {

    //@Disabled("See https://github.com/quarkusio/quarkus/issues/47760")
    @Test
    public void testTaskShouldUseTestFixtures() throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("test-fixtures-module-with-package-private-parent");

        final BuildResult result = runGradleWrapper(projectDir, "clean", "test");

        assertThat(BuildResult.isSuccessful(result.getTasks().get(":test"))).isTrue();
    }
}
