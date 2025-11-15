package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * Tests compatibility of the KSP plugin with sourcesJar task.
 * Previously, the Quarkus plugin added the codegen sources to the main sources and that created a cyclic task dependencies.
 */
public class KspPluginWithSourcesJarTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testKspWithSourcesJar() throws Exception {
        final File projectDir = getProjectDir("kotlin-ksp");
        runGradleWrapper(projectDir, "clean", "build");
    }
}
