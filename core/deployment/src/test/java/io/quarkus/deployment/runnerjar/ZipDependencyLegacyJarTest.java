package io.quarkus.deployment.runnerjar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.TsArtifact;

public class ZipDependencyLegacyJarTest extends BootstrapFromOriginalJarTestBase {

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
        final Path libDir = app.getJar().getLibraryDir();
        assertTrue(Files.isDirectory(libDir));
        final Set<String> actualLib = getDirContent(libDir);
        assertThat(actualLib).contains(
                "io.quarkus.bootstrap.test.zip-dep-1.zip",
                "io.quarkus.bootstrap.test.jar-dep-1.jar");
    }

    protected Set<String> getDirContent(Path dir) throws IOException {
        final Set<String> content = new HashSet<>();
        try (Stream<Path> stream = Files.list(dir)) {
            final Iterator<Path> i = stream.iterator();
            while (i.hasNext()) {
                content.add(i.next().getFileName().toString());
            }
        }
        return content;
    }

    @Override
    protected Properties buildSystemProperties() {
        var props = new Properties();
        props.setProperty("quarkus.package.jar.type", "legacy-jar");
        return props;
    }
}
