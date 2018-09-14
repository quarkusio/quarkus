package org.jboss.shamrock.runner;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;

import org.jboss.shamrock.deployment.ArchiveContextBuilder;
import org.jboss.shamrock.deployment.BuildTimeGenerator;

/**
 * Class that can be used to run shamrock directly, ececuting the build and runtime
 * steps in the same JVM
 */
public class RuntimeRunner implements Runnable, Closeable {

    private final Path target;
    private final RuntimeClassLoader loader;
    private Closeable closeTask;
    private final ArchiveContextBuilder archiveContextBuilder;

    public RuntimeRunner(ClassLoader classLoader, Path target, Path frameworkClassesPath, ArchiveContextBuilder archiveContextBuilder) {
        this.target = target;
        this.archiveContextBuilder = archiveContextBuilder;
        this.loader = new RuntimeClassLoader(classLoader, target, frameworkClassesPath);
    }

    @Override
    public void close() throws IOException {
        if (closeTask != null) {
            closeTask.close();
        }
    }

    @Override
    public void run() {
        try {
            BuildTimeGenerator buildTimeGenerator = new BuildTimeGenerator(loader, loader, false, archiveContextBuilder);
            buildTimeGenerator.run(target);
            loader.accept(buildTimeGenerator.getBytecodeTransformers());
            Class<?> mainClass = loader.findClass(BuildTimeGenerator.MAIN_CLASS);
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(loader);

                Method run = mainClass.getDeclaredMethod("main", String[].class);
                run.invoke(null, (Object) null);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }

            Method close = mainClass.getDeclaredMethod("close");

            closeTask = new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        close.invoke(null);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
