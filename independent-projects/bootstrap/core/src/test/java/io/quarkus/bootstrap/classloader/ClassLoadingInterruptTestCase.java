package io.quarkus.bootstrap.classloader;

import io.quarkus.bootstrap.classloading.DirectoryClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.util.IoUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassLoadingInterruptTestCase {

    @Test
    public void testClassLoaderWhenThreadInterrupted() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .addClasses(ClassToLoad.class, InterruptClass.class);
        Path path = Files.createTempDirectory("test");
        try {
            jar.as(ExplodedExporter.class).exportExploded(path.toFile(), "tmp");

            ClassLoader cl = QuarkusClassLoader.builder("test", getClass().getClassLoader(), false)
                    .addElement(new DirectoryClassPathElement(path.resolve("tmp")))
                    .build();
            Class<?> c = cl.loadClass(InterruptClass.class.getName());
            Assertions.assertNotEquals(c, InterruptClass.class);
            Runnable runnable = (Runnable) c.getDeclaredConstructor().newInstance();
            runnable.run();
        } finally {
            IoUtils.recursiveDelete(path);
        }

    }
}
