package io.quarkus.deployment.builditem.nativeimage;

import java.util.Comparator;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A system property that will be set at native image build time
 */
public final class NativeImageSystemPropertyBuildItem extends MultiBuildItem
        implements Comparable<NativeImageSystemPropertyBuildItem> {

    private static final Comparator<NativeImageSystemPropertyBuildItem> COMPARATOR = Comparator
            .comparing((NativeImageSystemPropertyBuildItem item) -> item.key)
            .thenComparing(item -> item.value);

    private final String key;
    private final String value;

    public NativeImageSystemPropertyBuildItem(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int compareTo(NativeImageSystemPropertyBuildItem other) {
        return COMPARATOR.compare(this, other);
    }
}
