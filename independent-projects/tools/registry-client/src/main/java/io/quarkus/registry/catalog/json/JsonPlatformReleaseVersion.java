package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import java.util.Objects;

@Deprecated
@JsonSerialize(using = JsonPlatformReleaseVersionSerializer.class)
@JsonDeserialize(using = JsonPlatformReleaseVersionDeserializer.class)
public class JsonPlatformReleaseVersion implements PlatformReleaseVersion {

    public static JsonPlatformReleaseVersion fromString(String s) {
        return new JsonPlatformReleaseVersion(s);
    }

    private final String version;

    private JsonPlatformReleaseVersion(String version) {
        this.version = Objects.requireNonNull(version);
    }

    @JsonIgnore
    public String getVersion() {
        return version;
    }

    @Override
    public int compareTo(PlatformReleaseVersion o) {
        return version.compareTo(o.getVersion());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof PlatformReleaseVersion)) {
            return false;
        }
        PlatformReleaseVersion that = (PlatformReleaseVersion) o;
        return Objects.equals(version, that.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public String toString() {
        return version;
    }
}
