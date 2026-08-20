package io.quarkus.bootstrap.app;

import java.util.Collections;
import java.util.Map;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * The result of building an augmentation class loader: the class loader itself
 * and the element cache populated during construction (for reuse by runtime class loader builds).
 */
public record AugmentationClassLoaderResult(QuarkusClassLoader classLoader,
        Map<ArtifactKey, ClassPathElement> elementCache) {

    public AugmentationClassLoaderResult(QuarkusClassLoader classLoader,
            Map<ArtifactKey, ClassPathElement> elementCache) {
        this.classLoader = classLoader;
        this.elementCache = Collections.unmodifiableMap(elementCache);
    }
}
