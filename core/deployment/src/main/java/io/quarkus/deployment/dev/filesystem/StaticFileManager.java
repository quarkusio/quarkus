package io.quarkus.deployment.dev.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.jboss.logging.Logger;

/**
 * This static file manager handle the class-paths and file locations of a single manager instance.
 */
public class StaticFileManager extends QuarkusFileManager {

    private final Context context;
    private final AtomicBoolean once = new AtomicBoolean();

    public StaticFileManager(Supplier<StandardJavaFileManager> supplier, Context context) {
        super(supplier.get(), context);
        this.context = context;
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaSources(Iterable<? extends File> files) {
        return this.fileManager.getJavaFileObjectsFromFiles(files);
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind)
            throws IOException {
        JavaFileObject file = this.fileManager.getJavaFileForInput(location, className, kind);
        // Ignore the module info of the application in dev mode.
        if (file != null && context.ignoreModuleInfo() && "CLASS_OUTPUT".equalsIgnoreCase(location.getName())
                && "module-info".equalsIgnoreCase(className)) {
            if (once.compareAndSet(false, true)) {
                Logger.getLogger(StaticFileManager.class).info("Ignoring module-info.java in dev mode, " +
                        "set the `quarkus.live-reload.ignore-module-info` property to `false` in your project descriptor (`pom.xml` or `build.gradle`) to disable this behavior.");
            }
            return null;
        }
        return file;
    }

}
