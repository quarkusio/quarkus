package io.quarkus.deployment.dev.filesystem;

import java.io.File;
import java.util.function.Supplier;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * This static file manager handle the class-paths and file locations of a single manager instance.
 */
public class StaticFileManager extends QuarkusFileManager {

    public StaticFileManager(Supplier<StandardJavaFileManager> supplier, Context context) {
        super(supplier.get(), context);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaSources(Iterable<? extends File> files) {
        return this.fileManager.getJavaFileObjectsFromFiles(files);
    }
}
