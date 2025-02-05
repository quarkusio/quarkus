package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.configuration.ClassLoadingConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;

public class ApplicationIndexBuildStep {

    private static final Logger log = Logger.getLogger(ApplicationIndexBuildStep.class);

    @BuildStep
    ApplicationIndexBuildItem build(ArchiveRootBuildItem root, CurateOutcomeBuildItem curation,
            ClassLoadingConfig classLoadingConfig) throws IOException {
        Indexer indexer = new Indexer();
        Set<String> removedApplicationClasses = removedApplicationClasses(curation, classLoadingConfig);
        for (Path p : root.getRootDirectories()) {
            Files.walkFileTree(p, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().endsWith(".class")) {
                        if (isRemovedApplicationClass(file, removedApplicationClasses)) {
                            log.debugf("File %s will not be indexed because the class has been configured as part of '%s'",
                                    file, "quarkus.class-loading.removed-resources");
                        } else {
                            log.debugf("Indexing %s", file);
                            try (InputStream stream = Files.newInputStream(file)) {
                                indexer.index(stream);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                private boolean isRemovedApplicationClass(Path file, Set<String> removedApplicationClasses) {
                    if (removedApplicationClasses.isEmpty()) {
                        return false;
                    }
                    String fileName = file.toString().replace('\\', '/');
                    String sanitizedFileName = (fileName.startsWith("/") ? fileName.substring(1) : fileName);
                    for (String removedApplicationClass : removedApplicationClasses) {
                        if (sanitizedFileName.endsWith(removedApplicationClass)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Index appIndex = indexer.complete();
        return new ApplicationIndexBuildItem(appIndex);
    }

    private Set<String> removedApplicationClasses(CurateOutcomeBuildItem curation, ClassLoadingConfig classLoadingConfig) {
        ResolvedDependency appArtifact = curation.getApplicationModel().getAppArtifact();
        Set<String> entry = classLoadingConfig.removedResources()
                .get(appArtifact.getGroupId() + ":" + appArtifact.getArtifactId());
        return entry != null ? entry : Collections.emptySet();
    }

}
