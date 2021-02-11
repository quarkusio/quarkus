package io.quarkus.registry.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.dependencies.Category;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableRegistry.class)
public interface Registry {

    @JsonProperty("core-versions")
    @Value.ReverseOrder
    SortedMap<ComparableVersion, Map<String, String>> getCoreVersions();

    Set<Extension> getExtensions();

    Set<Platform> getPlatforms();

    Set<Category> getCategories();

    static ImmutableRegistry.Builder builder() {
        return ImmutableRegistry.builder();
    }
}
