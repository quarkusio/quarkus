package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.TsArtifact;

public class ZipDependencyUberJarTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {
        final TsArtifact zipDep = TsArtifact.zip("zip-dep");
        final TsArtifact jarDep = TsArtifact.jar("jar-dep");

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(zipDep)
                .addDependency(jarDep);
    }

    @Override
    protected void testBootstrap(QuarkusBootstrap creator) throws Exception {
        final CuratedApplication curated = creator.bootstrap();
        AugmentResult app = curated.createAugmentor().createProductionApplication();
        final Path runnerJar = app.getJar().getPath();
        assertTrue(Files.exists(runnerJar));

        try (JarFile jar = new JarFile(runnerJar.toFile())) {
            JarEntry zipDepEntry = jar
                    .getJarEntry("META-INF/maven/io.quarkus.bootstrap.test/zip-dep/pom.properties");
            assertNotNull(zipDepEntry, "Expected zip-dep's content to be merged into the uber jar");

            JarEntry jarDepEntry = jar
                    .getJarEntry("META-INF/maven/io.quarkus.bootstrap.test/jar-dep/pom.properties");
            assertNotNull(jarDepEntry, "Expected jar-dep's content to be merged into the uber jar");
        }
    }

    @Override
    protected Properties buildSystemProperties() {
        var props = new Properties();
        props.setProperty("quarkus.package.jar.type", "uber-jar");
        return props;
    }
}
