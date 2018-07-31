package org.jboss.shamrock.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;

import org.jboss.shamrock.deployment.BuildTimeGenerator;

public class RuntimeRunner implements Runnable, Closeable {

    private final Path target;
    private final RuntimeClassLoader loader;
    private Closeable closeTask;

    public RuntimeRunner(Path target) {
        this.target = target;
        this.loader = new RuntimeClassLoader(getClass().getClassLoader());
    }

    public RuntimeRunner(Path target, ClassLoader cl) {
        this.target = target;
        this.loader = new RuntimeClassLoader(cl);
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
            BuildTimeGenerator buildTimeGenerator = new BuildTimeGenerator(loader, loader, false);
            buildTimeGenerator.run(target);
            Class<?> mainClass = loader.findClass(BuildTimeGenerator.MAIN_CLASS);
            Method run = mainClass.getDeclaredMethod("main", String[].class);
            run.invoke(null, (Object) null);

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
