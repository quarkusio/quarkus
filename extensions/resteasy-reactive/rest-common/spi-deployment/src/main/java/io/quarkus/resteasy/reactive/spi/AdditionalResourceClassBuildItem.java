package io.quarkus.resteasy.reactive.spi;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Can be used by extensions that want to make classes not annotated with JAX-RS {@code @Path}
 * part of the ResourceClass scanning process.
 * This will likely be used in conjunction with {@code io.quarkus.resteasy.reactive.server.spi.AnnotationsTransformerBuildItem}
 */
public final class AdditionalResourceClassBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;
    private final String path;

    public AdditionalResourceClassBuildItem(ClassInfo classInfo, String path) {
        this.classInfo = classInfo;
        this.path = path;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public String getPath() {
        return path;
    }
}
