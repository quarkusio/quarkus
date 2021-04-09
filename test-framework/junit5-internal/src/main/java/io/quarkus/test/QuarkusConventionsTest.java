package io.quarkus.test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppDependency;

/**
 * A test extension only meant for validating extensions use Quarkus class correctly
 */
public class QuarkusConventionsTest
        implements BeforeAllCallback, InvocationInterceptor {

    private IndexView extensionIndex = null;

    private void exportArchive(Path deploymentDir, Class<?> testClass) {
        try {
            JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
            Class<?> c = testClass;
            archive.addClasses(c.getClasses());
            while (c != Object.class) {
                archive.addClass(c);
                c = c.getSuperclass();
            }
            archive.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.toFile());
        } catch (Exception e) {
            throw new RuntimeException("Unable to create the archive", e);
        }
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (extensionIndex == null) {
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        Class<?> testClass = extensionContext.getRequiredTestClass();

        try {
            Path deploymentDir = Files.createTempDirectory("quarkus-architecture-test");
            exportArchive(deploymentDir, testClass);
            Path extensionDeploymentLocation = Paths.get("").toAbsolutePath();
            // we only want this run for the -deployment part of extensions
            if (!extensionDeploymentLocation.getFileName().toString().equals("deployment")) {
                return;
            }
            Path extensionLocation = extensionDeploymentLocation.getParent();
            QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                    .setApplicationRoot(deploymentDir)
                    .setMode(QuarkusBootstrap.Mode.TEST)
                    .addExcludedPath(extensionDeploymentLocation)
                    .setProjectRoot(extensionDeploymentLocation);
            try (CuratedApplication curatedApplication = builder.build().bootstrap()) {
                Indexer indexer = new Indexer();
                List<AppDependency> fullDeploymentDeps = curatedApplication.getAppModel().getFullDeploymentDeps();
                for (AppDependency deploymentDep : fullDeploymentDeps) {
                    try {
                        Path artifactPath = deploymentDep.getArtifact().getPaths().getSinglePath();
                        if (!Files.isDirectory(artifactPath)) {
                            continue;
                        }
                        // we are interested in the the modules of this extension only
                        if (!artifactPath.getParent().getParent().getParent().equals(extensionLocation)) {
                            continue;
                        }
                        indexDirectory(indexer, artifactPath);
                    } catch (IllegalStateException ignored) {

                    }
                }
                extensionIndex = indexer.complete();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void indexDirectory(Indexer indexer, final Path path) throws IOException {
        Files.walkFileTree(path, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!file.toString().endsWith(".class")) {
                    return FileVisitResult.CONTINUE;
                }
                try (InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {
                    indexer.index(inputStream);
                } catch (Exception e) {
                    // ignore
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public IndexView getExtensionIndex() {
        return extensionIndex;
    }
}
