package org.jboss.shamrock.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;

import org.jboss.shamrock.deployment.Runner;

public class RuntimeRunner implements Runnable, Closeable {

    private final Path target;
    private final RuntimeClassLoader loader;

    public RuntimeRunner(Path target) {
        this.target = target;
        this.loader = new RuntimeClassLoader(getClass().getClassLoader());
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void run() {
        try {
            Runner runner = new Runner(loader, false);
            runner.run(target);
            Class<?> mainClass = loader.findClass(Runner.MAIN_CLASS);
            Method run = mainClass.getDeclaredMethod("main", String[].class);
            run.invoke(null, (Object) null);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
