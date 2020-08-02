package io.quarkus.registry.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ImmutablePlatform.class)
public interface Platform {

    @JsonUnwrapped
    ArtifactKey getId();

    @Value.Auxiliary
    Set<Release> getReleases();
}
