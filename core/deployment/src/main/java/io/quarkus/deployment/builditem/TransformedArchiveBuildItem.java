package io.quarkus.deployment.builditem;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.ApplicationArchive;

/**
 * Represents all archives that contain classes that need to be transformed.
 *
 * In an environment that is using {@link io.quarkus.runner.RuntimeClassLoader} all classes
 * from these archives need to be loaded by the RuntimeClassLoader. They also need to be treated
 * as application classes, so all beans and other resources derived from them must also be loaded
 * into the same class loader.
 */
public final class TransformedArchiveBuildItem extends SimpleBuildItem {

    private final List<ApplicationArchive> applicationArchives;
    private final Map<String, Path> classFileToPath;
    private final Set<String> classes;

    public TransformedArchiveBuildItem(List<ApplicationArchive> applicationArchives, Map<String, Path> classFileToPath,
            Set<String> applicationJavaClassNames) {
        this.applicationArchives = applicationArchives;
        this.classFileToPath = classFileToPath;
        this.classes = applicationJavaClassNames;
    }

    public List<ApplicationArchive> getApplicationArchives() {
        return applicationArchives;
    }

    public Map<String, Path> getClassFileToPath() {
        return classFileToPath;
    }

    public Set<String> getClasses() {
        return classes;
    }
}
