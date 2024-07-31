package io.quarkus.annotation.processor.documentation.config.discovery;

import io.quarkus.annotation.processor.documentation.config.model.Extension;

public final class DiscoveryConfigGroup extends DiscoveryRootElement {

    public DiscoveryConfigGroup(Extension extension, String binaryName, String qualifiedName, boolean configMapping) {
        super(extension, binaryName, qualifiedName, configMapping);
    }

}
