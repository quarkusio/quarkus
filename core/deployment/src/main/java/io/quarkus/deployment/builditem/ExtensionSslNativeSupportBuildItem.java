package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Feature;

/**
 * A build item indicating that a specific Quarkus extension requires SSL support in native mode.
 * <p>
 * This is a {@link MultiBuildItem}. Each instance signifies that the extension named in the
 * {@link #extension} field needs the necessary native SSL configuration (like certificates)
 * included in the native image build. It can be instantiated using either a {@link Feature}
 * enum constant or directly with the extension's name string.
 */
public final class ExtensionSslNativeSupportBuildItem extends MultiBuildItem {

    private String extension;

    public ExtensionSslNativeSupportBuildItem(Feature feature) {
        this(feature.getName());
    }

    public ExtensionSslNativeSupportBuildItem(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
