package io.quarkus.maven.extension.deployment.metadata.model.spring;

import java.util.List;

public record SpringConfigMetadataModel(List<SpringConfigMetadataGroup> groups,
        List<SpringConfigMetadataProperty> properties,
        List<SpringConfigMetadataHint> hints) {

    public static SpringConfigMetadataModel empty() {
        return new SpringConfigMetadataModel(List.of(), List.of(), List.of());
    }
}
