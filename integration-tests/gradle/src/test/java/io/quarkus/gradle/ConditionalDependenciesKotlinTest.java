package io.quarkus.gradle;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

public class ConditionalDependenciesKotlinTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void buildProject() throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("conditional-dependencies-kotlin");

        runGradleWrapper(projectDir, "clean", "build");
    }
}
