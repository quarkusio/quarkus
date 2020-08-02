package io.quarkus.registry.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableArtifactKey.class)
public interface ArtifactKey {
    @Value.Parameter
    @JsonProperty("group-id")
    String getGroupId();

    @Value.Parameter
    @JsonProperty("artifact-id")
    String getArtifactId();
}
