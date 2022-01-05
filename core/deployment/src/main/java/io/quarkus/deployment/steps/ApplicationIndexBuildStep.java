package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;

public class ApplicationIndexBuildStep {

    private static final Logger log = Logger.getLogger(ApplicationIndexBuildStep.class);

    @BuildStep
    ApplicationIndexBuildItem build(ArchiveRootBuildItem root) throws IOException {
        Indexer indexer = new Indexer();
        for (Path p : root.getRootDirs()) {
            Files.walkFileTree(p, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".class")) {
                        log.debugf("Indexing %s", file);
                        try (InputStream stream = Files.newInputStream(file)) {
                            indexer.index(stream);
                        }
                    }
                    return FileVisitResult.CONTINUE;
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

}
