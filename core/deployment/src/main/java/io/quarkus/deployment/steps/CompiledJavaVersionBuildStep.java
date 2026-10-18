package io.quarkus.deployment.steps;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

public class CompiledJavaVersionBuildStep {

    private static final Logger log = Logger.getLogger(CompiledJavaVersionBuildStep.class);

    /**
     * Determines the Java version by looking up the major version of the first successfully parsed
     * application .class file that is found
     */
    @BuildStep
    public CompiledJavaVersionBuildItem compiledJavaVersion(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        final ResolvedDependency appArtifact = curateOutcomeBuildItem.getApplicationModel().getAppArtifact();
        try {
            Integer majorVersion = getMajorJavaVersion(appArtifact);
            if (majorVersion == null) {
                // workspace info isn't available in prod builds though
                for (ResolvedDependency module : curateOutcomeBuildItem.getApplicationModel()
                        .getDependencies(DependencyFlags.WORKSPACE_MODULE)) {
                    majorVersion = getMajorJavaVersion(module);
                    if (majorVersion != null) {
                        break;
                    }
                }
            }
            if (majorVersion == null) {
                log.debug("No .class files located");
                return CompiledJavaVersionBuildItem.unknown();
            }
            return CompiledJavaVersionBuildItem.fromMajorJavaVersion(majorVersion);
        } catch (Exception e) {
            log.warn("Failed to parse major version", e);
            return CompiledJavaVersionBuildItem.unknown();
        }

    }

    private static Integer getMajorJavaVersion(ResolvedDependency artifact) {
        final AtomicReference<Integer> majorVersion = new AtomicReference<>(null);
        artifact.getContentTree().walk(visit -> {
            final Path file = visit.getPath();
            if (file.getFileName() == null) {
                // this can happen if it's the root of a JAR
                return;
            }
            if (file.getFileName().toString().endsWith(".class") && !Files.isDirectory(file)) {
                log.debugf("Checking file '%s'", file.toAbsolutePath().toString());
                try (DataInputStream data = new DataInputStream(Files.newInputStream(file))) {
                    if (0xCAFEBABE == data.readInt()) {
                        data.readUnsignedShort(); // minor version -> we don't care about it
                        int v = data.readUnsignedShort();
                        majorVersion.set(v);
                        log.debugf("Determined compile java version to be %d", v);
                        visit.stopWalking();
                    }
                } catch (IOException e) {
                    log.debugf(e, "Encountered exception while processing file '%s'", file.toAbsolutePath().toString());
                }
            }
        });
        return majorVersion.get();
    }
}
