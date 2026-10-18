package io.quarkus.deployment.builditem;

import java.util.Objects;

import org.graalvm.nativeimage.hosted.Feature;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a GraalVM {@link Feature} to be passed to native-image through the {@code --features} options.
 */
public final class NativeImageFeatureBuildItem extends MultiBuildItem {

    private final String qualifiedName;

    public NativeImageFeatureBuildItem(Class<? extends Feature> featureClass) {
        this.qualifiedName = Objects.requireNonNull(featureClass).getName();
    }

    public NativeImageFeatureBuildItem(String qualifiedName) {
        this.qualifiedName = Objects.requireNonNull(qualifiedName);
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

}
