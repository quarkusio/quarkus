package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class DevDepsLeakIntoProdClaspathTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void test() throws Exception {
        final File projectDir = getProjectDir("dev-deps-leak-into-prod-48992");
        runGradleWrapper(projectDir, "dependencies", "--write-locks");
        runGradleWrapper(projectDir, "assemble");
    }
}
