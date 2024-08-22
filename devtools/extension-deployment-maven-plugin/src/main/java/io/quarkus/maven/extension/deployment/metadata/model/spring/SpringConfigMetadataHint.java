package io.quarkus.maven.extension.deployment.metadata.model.spring;

import java.util.List;

public record SpringConfigMetadataHint(String name, List<SpringConfigMetadataHintValue> values) {

}
