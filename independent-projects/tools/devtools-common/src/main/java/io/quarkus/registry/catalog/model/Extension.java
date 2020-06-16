package io.quarkus.registry.catalog.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.model.Release;
import java.util.List;
import org.immutables.value.Value;

/**
 * An extension is a Maven dependency that can be added to a Quarkus project
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableExtension.class)
public interface Extension {

    @JsonProperty("group-id")
    String getGroupId();

    @JsonProperty("artifact-id")
    String getArtifactId();

    @Value.Auxiliary
    List<Release> getReleases();

    static ImmutableExtension.Builder builder() {
        return ImmutableExtension.builder();
    }
}
