package io.quarkus.registry.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableArtifactCoords.class)
public interface ArtifactCoords {
    @Value.Parameter
    @JsonUnwrapped
    ArtifactKey getId();

    @Value.Parameter
    String getVersion();
}
