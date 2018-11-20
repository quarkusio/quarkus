package org.jboss.shamrock.test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import org.jboss.shamrock.runner.RuntimeRunner;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class ShamrockUnitTest extends BlockJUnit4ClassRunner {

    private Path deploymentDir;
    private RuntimeRunner runtimeRunner;

    static boolean started = false;

    public ShamrockUnitTest(Class<?> klass) throws InitializationError {
        super(klass);
        started = true;
    }

    @Override
    protected Object createTest() throws Exception {
        Object testInstance = super.createTest();
        return testInstance;
    }

    @Override
    protected Statement withBeforeClasses(Statement statement) {
        Statement existing = super.withBeforeClasses(statement);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                Class<?> theClass = getTestClass().getJavaClass();
                Method deploymentMethod = null;
                for (Method m : theClass.getMethods()) {
                    if (m.isAnnotationPresent(Deployment.class)) {
                        deploymentMethod = m;
                        break;
                    }
                }
                if (deploymentMethod == null) {
                    throw new RuntimeException("Could not find @Deployment method on " + theClass);
                }
                if (!Modifier.isStatic(deploymentMethod.getModifiers())) {
                    throw new RuntimeException("@Deployment method must be static" + deploymentMethod);
                }

                JavaArchive archive = (JavaArchive) deploymentMethod.invoke(null);
                deploymentDir = Files.createTempDirectory("shamrock-unit-test");

                archive.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.toFile());

                runtimeRunner = new RuntimeRunner(getClass().getClassLoader(), deploymentDir, deploymentDir, null, new ArrayList<>());
                runtimeRunner.run();
                existing.evaluate();

            }
        };
    }

    @Override
    protected Statement withAfterClasses(Statement statement) {
        Statement existing = super.withAfterClasses(statement);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    existing.evaluate();
                } finally {
                    try {
                        if (runtimeRunner != null) {
                            runtimeRunner.close();
                        }
                    } finally {
                        if (deploymentDir != null) {
                            Files.walkFileTree(deploymentDir, new FileVisitor<Path>() {
                                @Override
                                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    Files.delete(file);
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                    Files.delete(dir);
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        }
                    }
                }
            }
        };
    }
}
