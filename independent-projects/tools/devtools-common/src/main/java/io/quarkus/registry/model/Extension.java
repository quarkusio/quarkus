package io.quarkus.registry.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ImmutableExtension.class)
public interface Extension {

    @JsonUnwrapped
    ArtifactKey getId();

    @Value.Auxiliary
    String getName();

    @Value.Auxiliary
    @Nullable
    String getDescription();

    @Value.Auxiliary
    Map<String, Object> getMetadata();

    @Value.Auxiliary
    @Value.ReverseOrder
    SortedSet<ExtensionRelease> getReleases();

    @Value.Immutable
    @Value.Modifiable
    @JsonDeserialize(as = ImmutableExtensionRelease.class)
    interface ExtensionRelease extends Comparable<ExtensionRelease> {

        @JsonUnwrapped
        Release getRelease();

        @Value.Auxiliary
        Set<ExtensionPlatformRelease> getPlatforms();

        @Override
        default int compareTo(ExtensionRelease o) {
            return getRelease().compareTo(o.getRelease());
        }
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableExtensionPlatformRelease.class)
    interface ExtensionPlatformRelease {
        @JsonUnwrapped
        ArtifactCoords getCoords();

        @Value.Auxiliary
        Map<String, Object> getMetadata();
    }
}
