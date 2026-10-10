package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarFile;

import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsJar;

public class UberJarClosedFileSystemTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {
        var app = TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties());

        for (int i = 1; i <= 5; i++) {
            var libContent = new TsJar();
            for (int j = 1; j <= 50; j++) {
                libContent.addEntry("package org.acme.lib" + i + ";\npublic class Class" + j + " {}",
                        "org/acme/lib" + i + "/Class" + j + ".class");
                libContent.addEntry("key" + j + "=value" + j,
                        "org/acme/lib" + i + "/resource" + j + ".txt");
            }
            var lib = TsArtifact.jar("runtime-lib-" + i)
                    .setContent(libContent);
            app.addDependency(lib);
        }

        return app;
    }

    @Override
    protected void testBootstrap(QuarkusBootstrap creator) throws Exception {
        final CuratedApplication curated = creator.bootstrap();
        AugmentResult app = curated.createAugmentor().createProductionApplication();
        final Path runnerJar = app.getJar().getPath();
        assertTrue(Files.exists(runnerJar));
        try (JarFile jar = new JarFile(runnerJar.toFile())) {
            assertNotNull(jar.getEntry("org/acme/lib1/Class1.class"));
            assertNotNull(jar.getEntry("org/acme/lib1/resource1.txt"));
            assertNotNull(jar.getEntry("org/acme/lib2/Class1.class"));
        }
    }

    @Override
    protected Properties buildSystemProperties() {
        var props = new Properties();
        props.setProperty("quarkus.package.jar.type", "uber-jar");
        return props;
    }
}
