package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.TsArtifact;

/**
 * Verifies that the fast-jar generated Jar has a META-INF/MANIFEST.MF containing the
 * matching Add-Opens entries from the ModuleOpenBuildItem(s) defined by all available extensions.
 */
public class FastJarManifestEntryTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {
        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties());
    }

    @Override
    protected void testBootstrap(QuarkusBootstrap creator) throws Exception {
        final CuratedApplication curated = creator.bootstrap();
        AugmentResult app = curated.createAugmentor().createProductionApplication();
        final Path runnerJar = app.getJar().getPath();
        assertTrue(Files.exists(runnerJar));
        try (JarFile jar = new JarFile(runnerJar.toFile())) {
            final Attributes mainAttrs = jar.getManifest().getMainAttributes();
            assertEquals("java.base/java.lang", mainAttrs.getValue("Add-Opens"));
        }
    }

    @Override
    protected Properties buildSystemProperties() {
        var props = new Properties();
        props.setProperty("quarkus.package.jar.type", "fast-jar");
        return props;
    }

}
