package io.quarkus.registry.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableRelease.class)
public interface Release extends Comparable<Release> {

    String getVersion();

    @Nullable
    @JsonProperty("quarkus-core")
    String getQuarkusCore();

    @Nullable
    @JsonProperty("repository-url")
    @Value.Auxiliary
    String getRepositoryURL();

    static ImmutableRelease.Builder builder() {
        return ImmutableRelease.builder();
    }

    @Override
    default int compareTo(Release o) {
        int compare = new ComparableVersion(getVersion())
                .compareTo(new ComparableVersion(o.getVersion()));
        if (compare == 0 && (getQuarkusCore() != null && o.getQuarkusCore() != null)) {
            compare = new ComparableVersion(getQuarkusCore())
                    .compareTo(new ComparableVersion(o.getQuarkusCore()));
        }
        return compare;
    }
}
